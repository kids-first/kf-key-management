package io.kidsfirst.keys.core.model;

import java.util.Map;

import io.jsonwebtoken.Jwt;
import io.kidsfirst.keys.core.utils.JWTUtils;
import lombok.Data;

@Data
public class LambdaRequest {

  // === Lambda Will Populate Object ===
  private Map<String, String> body;
  private Map<String, String> headers;
  private Map<String, String> queryStringParameters;


  /**
   * getUserId will use the Ego JWT in the Authorization header to authorize the request (using Ego Public Key) and
   *  if authorized will return the user ID.
   * @return User ID found in the Ego JWT
   * @throws IllegalAccessException Indicates request is unauthorized, or is authorized but missing a userID
   */
  public String getUserId() throws IllegalAccessException {
    try {
      String auth = this.headers.getOrDefault("Authorization","");

      // Process header, will throw error if authorization fails
      String token = auth.replace("Bearer ", "");
      Jwt authToken = JWTUtils.parseToken(token, "ego");

      String userId = JWTUtils.getUserId(authToken);

      if (userId == null) {
        throw new IllegalAccessException("Authorization token is missing user ID.");
      }

      return userId;

    } catch (Exception e) {
      throw new IllegalAccessException(e.getMessage());

    }
  }

}
