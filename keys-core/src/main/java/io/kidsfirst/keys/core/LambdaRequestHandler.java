package io.kidsfirst.keys.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.kidsfirst.keys.core.exception.NotFoundException;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import lombok.val;
import lombok.var;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;

public abstract class LambdaRequestHandler implements RequestHandler<LambdaRequest, LambdaResponse> {

    public abstract LambdaResponse processEvent(final LambdaRequest request) throws Exception;

    public LambdaResponse handleRequest(final LambdaRequest input, final Context context) {
        val start = System.currentTimeMillis();
        LambdaLogger logger = context.getLogger();
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
            logException(logger, input, e);
            resp.setStatusCode((HttpURLConnection.HTTP_INTERNAL_ERROR));
            resp.setBody(formatException(e).toJSONString());

        }

        val end = System.currentTimeMillis();
        val durationMs = start - end;
        logger.log("Execution Duration of lambda (ms) = " + durationMs);
        return resp;

    }

    private static void logException(LambdaLogger logger, LambdaRequest input, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        logger.log("Error occured : input=" + input.getBody() + ", stack trace=" + sw.toString());
    }

    private static JSONObject formatException(Exception e) {
        val output = new JSONObject();
        output.put("error", e.getMessage());

        // Add Stack Trace if environment states debug
        val debug = System.getenv("debug");
        if (debug != null && debug.equalsIgnoreCase("true")) {
            val sb = new StringBuffer();
            output.put("exception", e.getClass().getSimpleName());
            for (StackTraceElement ste : e.getStackTrace()) {
                sb.append(ste.getClassName() + "." + ste.getMethodName() + ": " + ste.getLineNumber() + "\n");
            }
            output.put("stack", sb.toString());

        }

        return output;
    }

}
