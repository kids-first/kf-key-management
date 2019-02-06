package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.exception.NotFoundException;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.FenceUtils;
import lombok.val;
import org.json.simple.JSONObject;

import java.net.HttpURLConnection;

public class GetTokens extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request)
      throws IllegalAccessException, IllegalArgumentException, NotFoundException
  {

    val userId = request.getUserId();

    val fenceKey = request.getQueryStringValue("fence");
    val fence = FenceUtils.getProvider(fenceKey);

    val accessToken = FenceUtils.fetchAccessToken(fence, userId);
    val refreshToken = FenceUtils.fetchRefreshToken(fence, userId);

    if (!accessToken.isPresent() || !refreshToken.isPresent()) {
      throw new NotFoundException(String.format("No token for Fence: %s", fenceKey));
    }

    val body = new JSONObject();
    body.put("access_token", accessToken.orElse(""));
    body.put("refresh_token", refreshToken.orElse(""));

    val resp = new LambdaResponse();
    resp.addDefaultHeaders();
    resp.setBody(body.toJSONString());
    resp.setStatusCode(HttpURLConnection.HTTP_OK);
    return resp;

  }
}
