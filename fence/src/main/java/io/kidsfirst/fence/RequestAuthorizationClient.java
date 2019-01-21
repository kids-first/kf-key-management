package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

public class RequestAuthorizationClient extends LambdaRequestHandler {

    @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException, IOException {

    //No UserID check - no auth required

    LambdaResponse resp = new LambdaResponse();
    resp.addDefaultHeaders();

    JSONObject body = new JSONObject();

      body.put("client_id", Utils.getAuthClient().getClientId());
      body.put("redirect_uri", Utils.getAuthClient().getRedirectUri());
      body.put("scope", Utils.getAuthClient().getScope());

      resp.setBody(body.toJSONString());

    resp.setStatusCode(HttpURLConnection.HTTP_OK);

    return resp;
  }
}
