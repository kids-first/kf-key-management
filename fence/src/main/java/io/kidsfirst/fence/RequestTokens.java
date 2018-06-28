package io.kidsfirst.fence;

import com.auth0.jwt.JWT;
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
import lombok.val;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.kidsfirst.fence.Constants.ENV_FENCE_TOKEN_ENDPOINT;

public class RequestTokens extends LambdaRequestHandler {
    @Override
    public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException {

        JSONParser parser = new JSONParser();
        JSONObject headers = (JSONObject) parser.parse((String) event.get("headers"));
        val cookies =
            Stream.of( headers.get("cookie").toString().split(";") )
                .map(item -> item.split("="))
                .collect(
                    Collectors.toMap(
                            (kv) -> kv[0],
                            (kv) -> kv[1]
                    )
                );

        JSONObject queryParas = (JSONObject) parser.parse(event.get("queryStringParameters").toString());
        val auth_code = queryParas.get("code").toString();

        val tokens = requestTokens(
                auth_code,
                Utils.auth_client.clientId,
                Utils.auth_client.clientSecret,
                System.getProperty(ENV_FENCE_TOKEN_ENDPOINT),
                Utils.auth_client.redirectUri,
                Utils.auth_client.scope
        );

        Utils.persistTokens(
            JWTUtils.getUserId(JWTUtils.parseToken(tokens.getIDTokenString())),
            userId,
            tokens.getAccessToken().getValue(),
            tokens.getRefreshToken().getValue()
        );

        return null;
    }


    public OIDCTokens requestTokens(String auth_code, String client_id, String client_secret, String token_endpoint, String redirect_uri, String scope)  {

        try{
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

            if(resp.indicatesSuccess())
                return OIDCTokenResponse.parse(resp).toSuccessResponse().getOIDCTokens();

        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }  catch (com.nimbusds.oauth2.sdk.ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
