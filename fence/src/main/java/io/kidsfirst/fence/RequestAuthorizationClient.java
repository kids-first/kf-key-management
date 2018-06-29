package io.kidsfirst.fence;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RequestAuthorizationClient implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        outputStream.write(
                String.format(
                        "{\"client_id\": \"%s\",\"redirect_uri\": \"%s\",\"scope\": \"%s\"}",
                        Utils.getAuthClient().getClientId(),
                        Utils.getAuthClient().getRedirectUri(),
                        Utils.getAuthClient().getScope()
                ).getBytes()
        );
        outputStream.flush();
        outputStream.close();
    }
}
