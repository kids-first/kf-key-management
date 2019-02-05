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
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException, ParseException, IOException, URISyntaxException {

    val userId = request.getUserId();

    val fenceKey = request.getQueryStringValue("fence");
    val fence = FenceUtils.getProvider(fenceKey);
    val authClient = FenceUtils.getAuthClient(fence);

    val storedRefresh = FenceUtils.fetchRefreshToken(fence, userId);

    if(storedRefresh.isPresent()) {

      val refresh = storedRefresh.get();
      val clientId = authClient.getClientId();
      val clientSecret = authClient.getClientSecret();
      val fenceEndpoint = fence.getEndpoint();

      val tokensResponse = refreshTokens(refresh, clientId, clientSecret, fenceEndpoint);

      if(tokensResponse.isPresent()) {
        val tokens = tokensResponse.get();git
        FenceUtils.persistAccessToken(fence, userId, tokens.getAccessToken().getValue());
        FenceUtils.persistRefreshToken(fence, userId, tokens.getAccessToken().getValue());

        val body = new JSONObject();
        body.put("access_token", tokens.getAccessToken().getValue());
        body.put("refresh_token", tokens.getRefreshToken().getValue());

        val resp = new LambdaResponse();
        resp.addDefaultHeaders();
        resp.setBody(body.toJSONString());
        resp.setStatusCode(HttpURLConnection.HTTP_OK);
        return resp;

      } else {
        val resp = new LambdaResponse();
        resp.addDefaultHeaders();
        val body = new JSONObject();
        body.put("error","No response from fence.");
        resp.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        return resp;

      }

    } else {
      throw new IllegalArgumentException("Requested user has no stored refresh token.");

    }

  }

  public Optional<Tokens> refreshTokens(String refreshToken, String clientId, String clientSecret, String tokenEndpoint) throws URISyntaxException, IOException, ParseException {

    val request = new TokenRequest(
        new URI(tokenEndpoint),
        new ClientSecretBasic(
            new ClientID(clientId),
            new Secret(clientSecret)
        ),
        new RefreshTokenGrant(
            new RefreshToken(refreshToken)
        )
    );

    val http_resp = TokenResponse.parse(request.toHTTPRequest().send());

    val resp = http_resp.toSuccessResponse();

    if(resp.indicatesSuccess()) {
      return Optional.of(resp.getTokens());
    }

    return Optional.empty();

  }
}
