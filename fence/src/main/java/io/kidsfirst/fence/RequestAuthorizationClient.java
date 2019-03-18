package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.FenceUtils;
import lombok.val;
import org.json.simple.JSONObject;

import java.net.HttpURLConnection;

public class RequestAuthorizationClient extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalArgumentException {

    //No UserID check - no auth required

    LambdaResponse resp = new LambdaResponse();
    resp.addDefaultHeaders();

    val fenceKey = request.getQueryStringValue("fence");
    val fence = FenceUtils.getProvider(fenceKey);

    val authClient = FenceUtils.getAuthClient(fence);

    val body = new JSONObject();

    body.put("client_id", authClient.getClientId());
    body.put("redirect_uri", authClient.getRedirectUri());
    body.put("scope", authClient.getScope());
    // DO NOT RETURN SECRET >:O

    resp.setBody(body.toJSONString());

    resp.setStatusCode(HttpURLConnection.HTTP_OK);

    return resp;
  }
}
