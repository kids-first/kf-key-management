package io.kidsfirst.web.rest;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.FenceService;
import io.kidsfirst.core.service.SecretService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/fence")
@Slf4j
public class FenceResource {

    private final FenceService fenceService;
    private final SecretService secretService;

    public FenceResource(FenceService fenceService, SecretService secretService) {
        this.fenceService = fenceService;
        this.secretService = secretService;
    }

    @GetMapping("/{fence}/authenticated")
    public Mono<ResponseEntity<JSONObject>> getAuthClient(@PathVariable("fence") String fenceKey, JwtAuthenticationToken authentication) throws IllegalArgumentException {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        val defaultResponse = new JSONObject();
        defaultResponse.put("authenticated", false);

        Mono<Optional<Long>> refreshExpiration = secretService.getSecret(fence.keyRefreshToken(), userId)
                .filter(Secret::notExpired).map(Secret::getExpiration).map(Optional::of).defaultIfEmpty(Optional.empty());

        Mono<Optional<Long>> accessExpiration = secretService.getSecret(fence.keyAccessToken(), userId)
                .filter(Secret::notExpired).map(Secret::getExpiration).map(Optional::of).defaultIfEmpty(Optional.empty());

        Mono<Long> expiration = Mono.zip(refreshExpiration, accessExpiration).flatMap(t -> {
            val refreshOpt = t.getT1();
            val accessOpt = t.getT2();
            if (refreshOpt.isPresent() && accessOpt.isPresent()) {
                val exp = accessOpt.get().compareTo(refreshOpt.get()) > 0 ? accessOpt : refreshOpt;
                return Mono.just(exp.get());
            } else return refreshOpt
                        .map(Mono::just)
                        .orElseGet(() -> accessOpt.map(Mono::just).orElseGet(Mono::empty));
        });

        return expiration
                .map(e -> {
                    val body = new JSONObject();
                    body.put("authenticated", true);
                    body.put("expiration", e);
                    return ResponseEntity.ok(body);
                }).defaultIfEmpty(ResponseEntity.ok(defaultResponse));
    }

    @GetMapping("/{fence}/info")
    public Mono<JSONObject> getAuthClient(@PathVariable("fence") String fenceKey) throws IllegalArgumentException {
        val fence = fenceService.getFence(fenceKey);
        //No UserID check - no auth required
        val body = new JSONObject();
        body.put("client_id", fence.getClientId());
        body.put("redirect_uri", fence.getRedirectUri());
        body.put("proxy_uri", fence.getProxyUri());
        body.put("scope", fence.getScope());
        body.put("token_uri", fence.getTokenEndpoint());

        return Mono.just(body);
    }

    @GetMapping("/{fence}/exchange")
    public Mono<ResponseEntity<JSONObject>> requestTokens(@RequestParam("code") String authCode, @PathVariable("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        return fenceService.requestTokens(authCode, fence)
                .flatMap(t -> secretService.persistTokens(fence, userId, t)
                        .filter(s -> s.getService().equals(fence.keyRefreshToken()))
                        .next()
                        .map(s -> {
                            val b = new JSONObject();
                            b.put("expiration", s.getExpiration());
                            return ResponseEntity.ok(b);
                        })
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{fence}/token")
    public Mono<ResponseEntity<Object>> deleteToken(@PathVariable("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        return secretService.removeFenceTokens(fence, userId).then(Mono.just(ResponseEntity.ok().build()));
    }


}
