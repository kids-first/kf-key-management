package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.FenceUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.net.HttpURLConnection;
import java.util.HashMap;

import static io.kidsfirst.fence.Utils.*;

public class GetTokens extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException {

    String userId = request.getUserId();

    LambdaResponse resp = new LambdaResponse();
    resp.addDefaultHeaders();

    String fenceKey = request.getQueryStringValue("fence");
    FenceUtils.Provider fence = FenceUtils.getProvider(fenceKey);

    String accessToken = FenceUtils.getAccessToken(fence, userId);
    String refreshToken = FenceUtils.getRefreshToken(fence, userId);

    JSONObject body = new JSONObject();
    body.put("access_token", accessToken);
    body.put("refresh_token", refreshToken);

    resp.setBody(body.toJSONString());
    resp.setStatusCode(HttpURLConnection.HTTP_OK);
    return resp;

  }
}
