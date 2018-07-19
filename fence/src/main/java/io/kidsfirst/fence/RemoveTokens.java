package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import org.json.simple.JSONObject;

public class RemoveTokens extends LambdaRequestHandler {
    @Override
    public String processEvent(JSONObject event, String userId) throws Exception {
        return removeTokens(userId);
    }

    public String removeTokens(String userId) {
        return Utils.removeTokens(userId);
    }
}
