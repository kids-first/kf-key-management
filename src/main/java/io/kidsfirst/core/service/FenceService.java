package io.kidsfirst.core.service;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.config.AllFences;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.Optional;

@Component
@Slf4j
public class FenceService {
    private final AllFences fences;

    public FenceService(AllFences fences) {
        this.fences = fences;
    }

    public Mono<OIDCTokens> refreshTokens(String refreshToken, AllFences.Fence fence)  {
        Mono<Optional<OIDCTokens>> blockingWrapper = Mono.fromCallable(() -> {
            val clientId = fence.getClientId();
            val clientSecret = fence.getClientSecret();
            val fenceEndpoint = fence.getTokenEndpoint();

            val request = new TokenRequest(
                    new URI(fenceEndpoint),
                    new ClientSecretBasic(
                            new ClientID(clientId),
                            new com.nimbusds.oauth2.sdk.auth.Secret(clientSecret)
                    ),
                    new RefreshTokenGrant(
                            new RefreshToken(refreshToken)
                    )
            );

            val fenceResponse = request.toHTTPRequest().send();

            if (fenceResponse.indicatesSuccess()) {
                val tokens = OIDCTokenResponse
                        .parse(fenceResponse)
                        .toSuccessResponse()
                        .getOIDCTokens();

                return Optional.of(tokens);
            }

            return Optional.empty();
        });

        return blockingWrapper.subscribeOn(Schedulers.boundedElastic()).flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty));

    }

    public Mono<OIDCTokens> requestTokens(String authCode, AllFences.Fence fence) {
        Mono<Optional<OIDCTokens>> blockingWrapper = Mono.fromCallable(() -> {
        String clientId = fence.getClientId();
        String clientSecret = fence.getClientSecret();
        String fenceEndpoint = fence.getTokenEndpoint();
        String redirectUri = fence.getRedirectUri();
        val fenceRequest = new TokenRequest(
                new URI(fenceEndpoint),

                new ClientSecretBasic(
                        new ClientID(clientId),
                        new com.nimbusds.oauth2.sdk.auth.Secret(clientSecret)
                ),

                new AuthorizationCodeGrant(
                        new AuthorizationCode(authCode),
                        new URI(redirectUri)
                ),

                new Scope(fence.getScope())
        );
        val fenceResponse = fenceRequest.toHTTPRequest().send();

        if (fenceResponse.indicatesSuccess()) {
            val tokens = OIDCTokenResponse
                    .parse(fenceResponse)
                    .toSuccessResponse()
                    .getOIDCTokens();

            return Optional.of(tokens);
        } else {
            log.error("Error in  {} fence response during request tokens: status={}, content={}", fence.getName(), fenceResponse.getStatusCode(), fenceResponse.getContent());
            return Optional.empty();
        }});

        return blockingWrapper.subscribeOn(Schedulers.boundedElastic()).flatMap(o -> o.map(Mono::just).orElseGet(Mono::empty));
    }

    public AllFences.Fence getFence(final String key){
        return fences.get(key);
    }



}
