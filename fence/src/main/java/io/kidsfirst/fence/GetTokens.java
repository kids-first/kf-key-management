package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import static io.kidsfirst.fence.Utils.*;

public class GetTokens extends LambdaRequestHandler {
    @Override
    public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException {

        KfTokens tokens = retrieveTokens(userId);

        return String.format("{\"access_token\":\"%s\", \"refresh_token\":\"%s\"}", tokens.getAccess_token(), tokens.getRefresh_token());
    }
}
