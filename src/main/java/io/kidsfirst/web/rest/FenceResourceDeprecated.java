package io.kidsfirst.web.rest;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.kidsfirst.core.service.FenceService;
import io.kidsfirst.core.service.SecretService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RestController
@RequestMapping("/")
@Slf4j
public class FenceResourceDeprecated {

    private final FenceService fenceService;
    private final SecretService secretService;

    public FenceResourceDeprecated(FenceService fenceService, SecretService secretService) {
        this.fenceService = fenceService;
        this.secretService = secretService;
    }

    @GetMapping("/auth-client")
    public Mono<JSONObject> getAuthClient(@RequestParam("fence") String fenceKey) throws IllegalArgumentException {
        val fence = fenceService.getFence(fenceKey);
        //No UserID check - no auth required
        val body = new JSONObject();
        body.put("client_id", fence.getClientId());
        body.put("redirect_uri", fence.getRedirectUri());
        body.put("scope", fence.getScope());

        return Mono.just(body);
    }

    @GetMapping("/token")
    public Mono<ResponseEntity<JSONObject>> getTokens(@RequestParam("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();

        val fence = fenceService.getFence(fenceKey);

        val accessToken = secretService.fetchAccessToken(fence, userId);
        val refreshToken = secretService.fetchRefreshToken(fence, userId);

        return accessToken
                .zipWith(refreshToken)
                .map(Tuple2::getT1)
                .map(token -> {

                    val body = new JSONObject();
                    body.put("access_token", token);
                    return ResponseEntity.ok(body);

                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorReturn(ResponseEntity.notFound().build());

    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<JSONObject>> refresh(@RequestParam("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        val storedRefresh = secretService.fetchRefreshToken(fence, userId);
        return storedRefresh
                .flatMap(refresh -> fenceService.refreshTokens(refresh, fence))
                .flatMap(tokens -> {
                            val body = new JSONObject();
                            body.put("access_token", tokens.getAccessToken().getValue());
                            body.put("refresh_token", tokens.getRefreshToken().getValue());
                            return secretService.persistTokens(fence, userId, tokens)
                                    .then(Mono.just(ResponseEntity.ok().body(body)));
                        }
                ).defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @PostMapping("/token")
    public Mono<ResponseEntity<JSONObject>> requestTokens(@RequestParam("code") String
                                                                  authCode, @RequestParam("fence") String fenceKey, JwtAuthenticationToken authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        return fenceService.requestTokens(authCode, fence)
                .flatMap(t -> {
                    val body = new JSONObject();
                    body.put("access_token", t.getAccessToken().getValue());
                    body.put("refresh_token", t.getRefreshToken().getValue());
                    val response = ResponseEntity.ok().body(body);
                    return secretService.persistTokens(fence, userId, t, true).then(Mono.just(response));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());

    }

    @DeleteMapping("/token")
    public Mono<ResponseEntity<Object>> deleteToken(@RequestParam("fence") String fenceKey, JwtAuthenticationToken
            authentication) {
        val userId = authentication.getTokenAttributes().get("sub").toString();
        val fence = fenceService.getFence(fenceKey);
        return secretService.removeFenceTokens(fence, userId).then(Mono.just(ResponseEntity.ok().build()));
    }


}
