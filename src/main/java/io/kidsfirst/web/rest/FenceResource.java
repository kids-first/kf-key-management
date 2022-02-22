package io.kidsfirst.web.rest;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.service.FenceService;
import io.kidsfirst.core.service.SecretService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
        val isAuthenticated = secretService.fetchAccessToken(fence, userId).hasElement();
        return isAuthenticated.map(b -> {
            val body = new JSONObject();
            body.put("authenticate", b);
            return ResponseEntity.ok(body);
        });
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

        return Mono.just(body);
    }

    @GetMapping("/{fence}/exchange")
    public Mono<ResponseEntity<JSONObject>> requestTokens(@RequestParam("code") String authCode, @PathVariable("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        return fenceService.requestTokens(authCode, fence)
                .flatMap(t -> secretService.persistTokens(fence, userId, t).then(Mono.just(ResponseEntity.ok(new JSONObject()))))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{fence}/token")
    public Mono<ResponseEntity<Object>> deleteToken(@PathVariable("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        return secretService.removeFenceTokens(fence, userId).then(Mono.just(ResponseEntity.ok().build()));
    }


}
