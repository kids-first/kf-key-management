package io.kidsfirst.fence;

import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import io.kidsfirst.keys.core.LambdaRequestHandler;
import lombok.val;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static io.kidsfirst.fence.Constants.ENV_FENCE_TOKEN_ENDPOINT;
import static io.kidsfirst.fence.Utils.retrieveTokens;
import static io.kidsfirst.fence.Utils.updateTokens;

public class RefreshTokens extends LambdaRequestHandler {
    @Override
    public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException {
        val tokens =
                refreshTokens(
                        retrieveTokens(userId).refresh_token,
                        Utils.auth_client.clientId,
                        Utils.auth_client.clientSecret,
                        System.getProperty(ENV_FENCE_TOKEN_ENDPOINT)
                );

        updateTokens(userId, tokens.getAccessToken().getValue(), tokens.getRefreshToken().getValue());

        return tokens.getAccessToken().getValue();
    }

    public Tokens refreshTokens(String refresh_token, String client_id, String client_secret, String token_endpoint) {

        try{
            val request = new TokenRequest(
                    new URI(token_endpoint),
                    new ClientSecretBasic(
                            new ClientID(client_id),
                            new Secret(client_secret)
                    ),
                    new RefreshTokenGrant(
                            new RefreshToken(refresh_token)
                    )
            );

            val http_resp = TokenResponse.parse(request.toHTTPRequest().send());

            val resp = http_resp.toSuccessResponse();

            if(resp.indicatesSuccess())
                return resp.getTokens();

        } catch (com.nimbusds.oauth2.sdk.ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;

    }
}
