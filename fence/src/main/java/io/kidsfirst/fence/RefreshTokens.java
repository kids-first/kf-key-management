package io.kidsfirst.fence;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.FenceUtils;
import lombok.val;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class RefreshTokens extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request)
      throws IllegalAccessException, IllegalArgumentException, ParseException, IOException, URISyntaxException
  {

    val userId = request.getUserId();

    val fenceKey = request.getQueryStringValue("fence");
    val fence = FenceUtils.getProvider(fenceKey);
    val authClient = FenceUtils.getAuthClient(fence);

    val storedRefresh = FenceUtils.fetchRefreshToken(fence, userId);

    if(storedRefresh.isPresent()) {

      val refresh = storedRefresh.get();

      val tokensResponse = refreshTokens(refresh, authClient, fence);

      if(tokensResponse.isPresent()) {
        val tokens = tokensResponse.get();
        FenceUtils.persistAccessToken(fence, userId, tokens.getAccessToken().getValue());
        FenceUtils.persistRefreshToken(fence, userId, tokens.getRefreshToken().getValue());

        val body = new JSONObject();
        body.put("access_token", tokens.getAccessToken().getValue());
        body.put("refresh_token", tokens.getRefreshToken().getValue());

        val resp = new LambdaResponse();
        resp.addDefaultHeaders();
        resp.setBody(body.toJSONString());
        resp.setStatusCode(HttpURLConnection.HTTP_OK);
        return resp;

      } else {
        throw new IllegalArgumentException("Fence failed refresh attempt.");

      }

    } else {
      throw new IllegalArgumentException("Requested user has no stored refresh token.");

    }

  }

  public Optional<Tokens> refreshTokens(String refreshToken, FenceUtils.AuthClient authClient, FenceUtils.Provider fence) throws URISyntaxException, IOException, ParseException {

    val clientId = authClient.getClientId();
    val clientSecret = authClient.getClientSecret();
    val fenceEndpoint = fence.getEndpoint();

    val request = new TokenRequest(
        new URI(fenceEndpoint),
        new ClientSecretBasic(
            new ClientID(clientId),
            new Secret(clientSecret)
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
}
