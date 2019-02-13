package io.kidsfirst.keys.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.kidsfirst.keys.core.exception.NotFoundException;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import lombok.val;
import lombok.var;
import org.json.simple.JSONObject;

import java.net.HttpURLConnection;

public abstract class LambdaRequestHandler implements RequestHandler<LambdaRequest, LambdaResponse> {

  public abstract LambdaResponse processEvent(final LambdaRequest request) throws Exception;

  public LambdaResponse handleRequest(final LambdaRequest input, final Context context) {
    var resp = new LambdaResponse();
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

    } catch (ClassCastException e) {
      // Could not convert body parameter to expected type
      resp.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
      resp.setBody(formatException(e).toJSONString());

    } catch (NotFoundException e) {
      // Requested content is not available
      resp.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
      resp.setBody(formatException(e).toJSONString());
    } catch (Exception e) {
      resp.setStatusCode((HttpURLConnection.HTTP_INTERNAL_ERROR));
      resp.setBody(formatException(e).toJSONString());

    }

    return resp;

  }


  private static JSONObject formatException(Exception e) {
    val output = new JSONObject();
    output.put("error", e.getMessage());

    // Add Stack Trace if environment states debug
    val debug = System.getenv("debug");
    if( debug != null && debug.equalsIgnoreCase("true")) {
      val sb = new StringBuffer();
      output.put("exception", e.getClass().getSimpleName());
      for(StackTraceElement ste: e.getStackTrace()){
        sb.append(ste.getClassName() + "." + ste.getMethodName() + ": " + ste.getLineNumber() + "\n");
      }
      output.put("stack", sb.toString());

    }

    return output;
  }

}
