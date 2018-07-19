package io.kidsfirst.fence;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.model.LambdaResponse;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class RequestAuthorizationClient implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        LambdaResponse resp = new LambdaResponse();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        resp.setHeaders(headers);
        resp.setBase64Encoded(false);
        resp.setBody(String.format(
                "{\"client_id\": \"%s\",\"redirect_uri\": \"%s\",\"scope\": \"%s\"}",
                Utils.getAuthClient().getClientId(),
                Utils.getAuthClient().getRedirectUri(),
                Utils.getAuthClient().getScope()
        ));
        resp.setStatusCode(HttpURLConnection.HTTP_OK);

        JSONObject responseJson = resp.toJson();

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write(responseJson.toJSONString());
        writer.close();
    }
}
