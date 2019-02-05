package io.kidsfirst.fence;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.FenceUtils;
import lombok.var;

import java.net.HttpURLConnection;

public class RemoveTokens extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException {

    var userId = request.getUserId();

    var resp = new LambdaResponse();
    resp.addDefaultHeaders();

    var fenceKey = request.getQueryStringValue("fence");
    FenceUtils.Provider fence = FenceUtils.getProvider(fenceKey);

    FenceUtils.removeFenceTokens(fence, userId);

    resp.setStatusCode(HttpURLConnection.HTTP_OK);
    return resp;
  }

}
