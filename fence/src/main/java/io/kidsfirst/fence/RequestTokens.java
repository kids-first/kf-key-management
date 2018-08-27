package io.kidsfirst.fence;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.utils.JWTUtils;
import lombok.SneakyThrows;
import lombok.val;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;

import static io.kidsfirst.fence.Constants.ENV_FENCE_TOKEN_ENDPOINT;

public class RequestTokens extends LambdaRequestHandler {
  @Override
  public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException, NullPointerException {

    JSONParser parser = new JSONParser();

    Object strQueryPara = event.get("queryStringParameters");

    JSONObject queryParas = (JSONObject) parser.parse(strQueryPara.toString());

    val auth_code = queryParas.get("code");

    val tokens = requestTokens(
      auth_code.toString(),
      Utils.getAuthClient().clientId,
      Utils.getAuthClient().clientSecret,
      System.getenv(ENV_FENCE_TOKEN_ENDPOINT),
      Utils.getAuthClient().redirectUri,
      Utils.getAuthClient().scope
    );

    Utils.persistTokens(
      JWTUtils.getUserId(JWTUtils.parseToken(tokens.getIDTokenString(), "fence")),
      userId,
      tokens.getAccessToken().getValue(),
      tokens.getRefreshToken().getValue()
    );

    return String.format("{\"access_token\":\"%s\", \"refresh_token\":\"%s\", \"id_token\":\"%s\"}", tokens.getAccessToken().getValue(), tokens.getRefreshToken().getValue(), tokens.getIDTokenString());
  }

  @SneakyThrows
  public OIDCTokens requestTokens(String auth_code, String client_id, String client_secret, String token_endpoint, String redirect_uri, String scope) {
    val request = new TokenRequest(
      new URI(token_endpoint),

      new ClientSecretBasic(
        new ClientID(client_id),
        new Secret(client_secret)
      ),

      new AuthorizationCodeGrant(
        new AuthorizationCode(auth_code),
        new URI(redirect_uri)
      ),

      new Scope(scope)
    );
    val resp = request.toHTTPRequest().send();

    if (resp.indicatesSuccess()) {
      return OIDCTokenResponse.parse(resp).toSuccessResponse().getOIDCTokens();
    } else {
      throw new IllegalArgumentException(String.format("%s ; %s", resp.getStatusMessage(), resp.getContent()));
    }
  }
}
