package io.kidsfirst.core.service;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.core.model.Provider;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Component
@Slf4j
public class FenceService {
    private String obfuscate(String secret) {
        return secret.substring(0,3) + "*******";
    }
    public Optional<Tokens> refreshTokens(String refreshToken, Provider fence) throws URISyntaxException, IOException, ParseException {
        val clientId = fence.getClientId();
        val clientSecret = fence.getClientSecret();
        val fenceEndpoint = fence.getEndpoint();

        log.debug("request tokens, client_id={}, client_secret={}, fence_endpoint={}, refresh_token={}",
                clientId,
                obfuscate(clientSecret),
                fenceEndpoint,
                obfuscate(refreshToken));
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
    }

    public Optional<OIDCTokens> requestTokens(String authCode, Provider fence) throws ParseException, URISyntaxException, IOException {
        String clientId = fence.getClientId();
        String clientSecret = fence.getClientSecret();
        String fenceEndpoint = fence.getEndpoint();
        String redirectUri = fence.getRedirectUri();
        log.debug("request tokens, client_id={}, client_secret={}, fence_endpoint={}, auth_code={}, redirect_uri={}",
                clientId,
                obfuscate(clientSecret),
                fenceEndpoint,
                obfuscate(authCode),
                redirectUri);
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
            log.error("Error in  {} fence response during request tokens: status={}, content={}", fence.name(), fenceResponse.getStatusCode(), fenceResponse.getContent());
            return Optional.empty();
        }
    }

    public Provider getProvider(final String key) throws IllegalArgumentException {
        try {
            return Provider.valueOf(key.toUpperCase());

        } catch (IllegalArgumentException e) {
            // Override default message for unknown enum constant
            throw new IllegalArgumentException(String.format("Unknown fence identifier: %s", key), e);
        }
    }
}
