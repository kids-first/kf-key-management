package io.kidsfirst.keys.core.model;

import io.kidsfirst.keys.core.utils.JWTUtils;
import lombok.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

@Data()
public class LambdaRequest {

  // === Lambda Will Populate Object ===
  private String body;
  private Map<String, String> headers;
  private Map<String, String> queryStringParameters;

  // When needed, the body string is parsed to a JSONObject
  // Use custom getter methods (ex. getBodyValue) to access the values to ensure it is initialized before access
  @Getter(AccessLevel.PRIVATE)
  @Setter(AccessLevel.PRIVATE)
  private JSONObject bodyData = null;

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


  public String getBodyString(String key) throws IllegalArgumentException, ClassCastException {

    val output = (String) getBodyValue(key);

    return output;
  }

  public Object getBodyValue(String key) throws IllegalArgumentException {

    //Initialize bodyData if it hasnt been
    if(bodyData == null) {
      if (body == null) {
        throw new IllegalArgumentException(String.format("No Body Data Found. Expected Parameter '%s'.", key));
      }

      JSONParser parser = new JSONParser();

      try {
        bodyData = (JSONObject) parser.parse(body);
      } catch (ParseException e) {
        throw new IllegalArgumentException(String.format("Could not parse body as JSON."), e);
      }
    }

    if (!bodyData.containsKey(key)) {
      throw new IllegalArgumentException(String.format("No Parameter found for '%s' in body.", key));
    }

    val output = bodyData.get(key);

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
