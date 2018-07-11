package io.kidsfirst.keys.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.jsonwebtoken.Jwt;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.JWTUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class LambdaRequestHandler implements RequestStreamHandler {

  private static JSONParser parser = new JSONParser();

  public abstract String processEvent(JSONObject event, String userId) throws Exception;

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
    LambdaResponse resp = new LambdaResponse();
    String data;

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

      JSONObject event = (JSONObject)parser.parse(reader);

      String userId = getUserId(event);

      if (userId == null || userId.isEmpty()) {
        // Ensure we got a non-empty userId, otherwise throw error for illegal access
        throw new IllegalAccessException("Unable to identify user from authorization token.");
      }

      // ================================
      // !!! BEHOLD: Call to abstract !!!
      // ================================
      data = this.processEvent(event, userId);

      // If data comes back without error but is empty, we return 204, otherwise 200
      int responseCode = data.isEmpty() ? HttpURLConnection.HTTP_NO_CONTENT : HttpURLConnection.HTTP_OK;
      resp.setStatusCode(responseCode);

    } catch (ParseException e) {
      // Error parsing request body - return 400
      resp.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
      data = formatException(e).toJSONString();

    } catch (IllegalArgumentException e) {
      // Catchall used to indicate that we are missing required fields - return 400
      resp.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
      data = formatException(e).toJSONString();

    } catch (IllegalAccessException e) {
      // JWT/User ID issue, return unauthorized 401
      resp.setStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED);
      data = formatException(e).toJSONString();

    } catch (Exception e) {
      // Something unexpected happened - return 500
      resp.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
      data = formatException(e).toJSONString();
    }


    resp.setHeaders(buildHeaders());
    resp.setBase64Encoded(false);
    resp.setBody(data);
    JSONObject responseJson = resp.toJson();

    OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    writer.write(responseJson.toJSONString());
    writer.close();
  }

  private static Map<String, String> buildHeaders() {
    final Map<String, String> headers = new HashMap<>();

    String allowedDomains = System.getenv("corsAllowedDomains");
    if (allowedDomains == null || allowedDomains.isEmpty()) {
      allowedDomains = "*";
    }
    headers.put("Access-Control-Allow-Origin", allowedDomains);

    return headers;
  }

  private static String getUserId(JSONObject event) throws IllegalAccessException {

    try {
      String auth = (String)((JSONObject)event.get("headers")).getOrDefault("Authorization","");

      // Process header, will throw error if authorization fails
      Jwt authToken = JWTUtils.parseToken(auth.replace("Bearer ", ""));

      return JWTUtils.getUserId(authToken);

    } catch (Exception e) {
      throw new IllegalAccessException(e.getMessage());

    }
  }

//  private static JSONObject formatException(Exception e) {
//    JSONObject output = new JSONObject();
//    output.put("error", e.getClass().getSimpleName());
//    output.put("message", e.getMessage());
//    return output;
//  }

  private static JSONObject formatException(Exception e) {
    JSONObject output = new JSONObject();
    output.put("error", e.getClass().getSimpleName());
    output.put("message", e.getMessage());

    StringBuffer sb = new StringBuffer();
    for(StackTraceElement ste: e.getStackTrace()){
      sb.append(ste.getClassName() + "." + ste.getMethodName() + ": " + ste.getLineNumber() + "\n");
    }
    output.put("stack", sb.toString());

    return output;
  }

}
