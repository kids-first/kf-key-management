package io.kidsfirst.keys.core.model;

import io.kidsfirst.keys.core.utils.JWTUtils;
import lombok.Data;
import lombok.val;

import java.util.Map;

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
      val auth = this.headers.getOrDefault("Authorization","");

      // Process header, will throw error if authorization fails
      val token = auth.replace("Bearer ", "");
      val authToken = JWTUtils.parseToken(token, "ego");

      val userId = JWTUtils.getUserId(authToken);

      if (userId == null) {
        throw new IllegalAccessException("Authorization token is missing user ID.");
      }

      return userId;

    } catch (Exception e) {
      throw new IllegalAccessException(e.getMessage());

    }
  }


  public String getBodyValue(String key) throws IllegalArgumentException {

    if (body == null) {
      throw new IllegalArgumentException(String.format("No Body Data Found. Expected Parameter '%s'.", key));

    }

    val output = body.get(key);
    if (output.isEmpty()) {
      throw new IllegalArgumentException(String.format("No Parameter found for '%s' in body.", key));

    }

    return output;
  }

  public String getHeaderValue(String key) throws IllegalArgumentException {

    if (headers == null) {
      throw new IllegalArgumentException(String.format("No Headers Found. Expected '%s'.", key));

    }

    val output = headers.get(key);
    if (output.isEmpty()) {
      throw new IllegalArgumentException(String.format("No Header value found for '%s'.", key));
    }

    return output;
  }

  public String getQueryStringValue(String key) throws IllegalArgumentException {

    if (queryStringParameters == null) {
      throw new IllegalArgumentException(String.format("No URL query parameters Found. Expected '%s'.", key));

    }

    val output = queryStringParameters.get(key);
    if (output.isEmpty()) {
      throw new IllegalArgumentException(String.format("No Parameter value found for '%s' in URL query.", key));
    }

    return output;
  }
}
