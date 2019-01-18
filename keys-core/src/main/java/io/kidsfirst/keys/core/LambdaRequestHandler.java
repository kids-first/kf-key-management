package io.kidsfirst.keys.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;

public abstract class LambdaRequestHandler implements RequestHandler<LambdaRequest, LambdaResponse> {

  private static JSONParser parser = new JSONParser();

  public abstract LambdaResponse processEvent(final LambdaRequest request) throws Exception;

  public LambdaResponse handleRequest(final LambdaRequest input, final Context context) {
    LambdaResponse resp = new LambdaResponse();
    resp.addDefaultHeaders();

    try {
      // ================================
      // !!! BEHOLD: Call to abstract !!!
      // ================================
      resp = this.processEvent(input);

    } catch (IllegalAccessException e) {
      // JWT/User ID issue, return unauthorized 401
      resp.setStatusCode(HttpURLConnection.HTTP_UNAUTHORIZED);
      resp.setBody(formatException(e).toJSONString());

    } catch (IllegalArgumentException e) {
      // Catchall used to indicate that we are missing required fields - return 400
      resp.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
      resp.setBody(formatException(e).toJSONString());

    } catch (Exception e) {
      resp.setStatusCode((HttpURLConnection.HTTP_INTERNAL_ERROR));
      resp.setBody(formatException(e).toJSONString());

    }

    return resp;

  }


  private static JSONObject formatException(Exception e) {
    JSONObject output = new JSONObject();
    output.put("error", e.getClass().getSimpleName());
    output.put("message", e.getMessage());

    // Add Stack Trace if environment states debug
    if(System.getenv("debug") != null) {
      StringBuffer sb = new StringBuffer();
      for(StackTraceElement ste: e.getStackTrace()){
        sb.append(ste.getClassName() + "." + ste.getMethodName() + ": " + ste.getLineNumber() + "\n");
      }
      output.put("stack", sb.toString());

    }

    return output;
  }

}
