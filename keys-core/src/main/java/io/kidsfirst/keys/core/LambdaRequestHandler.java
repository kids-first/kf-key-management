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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public abstract class LambdaRequestHandler implements RequestStreamHandler {

  private static JSONParser parser = new JSONParser();

  public abstract String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException;

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
    LambdaResponse resp = new LambdaResponse();
    String data = "";


    try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      JSONObject event = (JSONObject)parser.parse(reader);

      String auth = (String)((JSONObject)event.get("headers")).getOrDefault("Authorization","");

      String userId = null;
      try {
        // Process header, will throw error if authorization fails
        Jwt authToken = JWTUtils.parseToken(auth.replace("Bearer ", ""));
        userId = JWTUtils.getUserId(authToken);

      } catch (Exception e) {
        // Authorization failure, return 403

        resp.setStatusCode("403");
        data = formatException(e).toJSONString();
      }

      if (userId == null || userId.isEmpty()) {
        throw new IllegalArgumentException("Unable to identify user from authorization token.");
      }

      // ================================
      // !!! BEHOLD: Call to abstract !!!
      // ================================
      data = this.processEvent(event, userId);
      resp.setStatusCode("200");

    } catch (ParseException e) {
      // Error parsing request body - return 400
      resp.setStatusCode("400");
      data = formatException(e).toJSONString();

    } catch (IllegalArgumentException e) {
      // Catchall used to indicate that we are missing required fields - return 400
      resp.setStatusCode("400");
      data = formatException(e).toJSONString();

    } catch (Exception e) {
      // Something unexpected happened - return 500
      resp.setStatusCode("500");
      data = formatException(e).toJSONString();
    }

    resp.setHeaders(new HashMap<>());
    resp.setBase64Encoded(false);
    resp.setBody(data);
    JSONObject responseJson = resp.toJson();

    OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    writer.write(responseJson.toJSONString());
    writer.close();
  }

  private static JSONObject formatException(Exception e) {
    JSONObject output = new JSONObject();
    output.put("error", e.getClass().getSimpleName());
    output.put("message", e.getMessage());
    return output;
  }

}
