package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import lombok.val;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class RequestAuthorizationClient extends LambdaRequestHandler {
    @Override
    public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException {

        val auth_client_info = Utils.auth_client;

        return
            String.format(
                    "{\"client_id\": %s,\"redirect_uri\": %s,\"scope\": %s}",
                    Utils.auth_client.getClientId(),
                    Utils.auth_client.getRedirectUri(),
                    Utils.auth_client.getScope()
            );
    }
}
