package io.kidsfirst.fence;

import io.kidsfirst.fence.dao.FenceTokenDao;
import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.exception.NotFoundException;
import lombok.val;
import org.json.simple.JSONObject;

import static io.kidsfirst.fence.Utils.decrypt;

public class GetTokens extends LambdaRequestHandler {
  @Override
  public String processEvent(JSONObject event, String userId) throws IllegalArgumentException {
    val tokens = FenceTokenDao.getSecret(userId);
    if (tokens.size() < 1) {
      throw new NotFoundException("No Token.");
    }

    val token = tokens.get(0);
    return String.format("{\"access_token\":\"%s\", \"refresh_token\":\"%s\"}", decrypt(token.getAccessToken()), decrypt(token.getRefreshToken()));
  }
}
