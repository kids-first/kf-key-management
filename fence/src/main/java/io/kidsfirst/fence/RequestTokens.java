package io.kidsfirst.fence;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.FenceUtils;
import io.kidsfirst.keys.core.utils.JWTUtils;
import lombok.val;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class RequestTokens extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request)
      throws IllegalAccessException, IllegalArgumentException, ParseException, IOException, URISyntaxException
  {

    val userId = request.getUserId();


    val authCode = request.getQueryStringValue("code");
    val fenceKey = request.getQueryStringValue("fence");
    val fence = FenceUtils.getProvider(fenceKey);

    val tokenResponse = requestTokens(authCode, fence);

    if(tokenResponse.isPresent()) {

      val tokens = tokenResponse.get();
      FenceUtils.persistTokens(fence, userId, tokens);

      val body = new JSONObject();
      body.put("access_token", tokens.getAccessToken().getValue());
      body.put("refresh_token", tokens.getRefreshToken().getValue());

      val resp = new LambdaResponse();
      resp.addDefaultHeaders();
      resp.setBody(body.toJSONString());
      resp.setStatusCode(HttpURLConnection.HTTP_OK);
      return resp;

    } else {
      throw new IllegalAccessException("Fence did not return tokens for the provided code.");
    }
  }


  public Optional<OIDCTokens> requestTokens(String authCode, FenceUtils.Provider fence) throws ParseException, URISyntaxException, IOException {

    val authClient = FenceUtils.getAuthClient(fence);
    val fenceRequest = new TokenRequest(
      new URI(fence.getEndpoint()),

      new ClientSecretBasic(
          new ClientID(authClient.getClientId()),
          new Secret(authClient.getClientSecret())
      ),

      new AuthorizationCodeGrant(
          new AuthorizationCode(authCode),
          new URI(authClient.getRedirectUri())
      ),

      new Scope(authClient.getScope())
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
}
