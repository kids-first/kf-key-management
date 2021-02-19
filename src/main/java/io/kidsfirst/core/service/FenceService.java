package io.kidsfirst.core.service;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.core.model.Provider;
import io.kidsfirst.core.model.Secret;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Component
public class FenceService {
  public Optional<Tokens> refreshTokens(String refreshToken, Provider fence) throws URISyntaxException, IOException, ParseException {
    val clientId = fence.getClientId();
    val clientSecret = fence.getClientSecret();
    val fenceEndpoint = fence.getEndpoint();

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

    if(fenceResponse.indicatesSuccess()) {
      val tokens = OIDCTokenResponse
              .parse(fenceResponse)
              .toSuccessResponse()
              .getOIDCTokens();

      return Optional.of(tokens);
    }

    return Optional.empty();
  }

  public Optional<OIDCTokens> requestTokens(String authCode, Provider fence) throws ParseException, URISyntaxException, IOException {
    val fenceRequest = new TokenRequest(
            new URI(fence.getEndpoint()),

            new ClientSecretBasic(
                    new ClientID(fence.getClientId()),
                    new com.nimbusds.oauth2.sdk.auth.Secret(fence.getClientSecret())
            ),

            new AuthorizationCodeGrant(
                    new AuthorizationCode(authCode),
                    new URI(fence.getRedirectUri())
            ),

            new Scope(fence.getScope())
    );
    val fenceResponse = fenceRequest.toHTTPRequest().send();

    if(fenceResponse.indicatesSuccess()) {
      val tokens = OIDCTokenResponse
              .parse(fenceResponse)
              .toSuccessResponse()
              .getOIDCTokens();

      return Optional.of(tokens);
    } else {
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
