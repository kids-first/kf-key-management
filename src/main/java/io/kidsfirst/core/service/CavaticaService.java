package io.kidsfirst.core.service;

import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.IntStream;

@Service
public class CavaticaService {

    private Environment env;

    private final int[] HTTP_SUCCESS_CODES = { HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED,
            HttpURLConnection.HTTP_ACCEPTED, HttpURLConnection.HTTP_NO_CONTENT, HttpURLConnection.HTTP_RESET };


    public CavaticaService(Environment env){
        this.env = env;
    }

    public String sendCavaticaRequest(String cavaticaKey, String path, String method, String body) throws IOException {
        String cavaticaRoot = env.getProperty("application.cavatica_root", env.getProperty("cavatica_root"));
        if(cavaticaRoot == null){
            throw new RuntimeException("cavatica_root not defined");
        }

        URL url = new URL( cavaticaRoot + path);

        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod(method);

        // standard connection setup
        con.setInstanceFollowRedirects(true);
        con.setConnectTimeout(1000);
        con.setReadTimeout(10000);

        // Add secret key
        con.setRequestProperty("X-SBG-Auth-Token", cavaticaKey);

        // Add body
        if (body != null) {
            con.setRequestProperty(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(body);
            out.flush();
            out.close();
        }

        int status = con.getResponseCode();

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder content = new StringBuilder();
        reader.lines().forEach(content::append);
        reader.close();
        String responseBody = content.toString();

        if (IntStream.of(HTTP_SUCCESS_CODES).noneMatch(code -> code == status)) {
            throw new IOException("Cavatica request failed. Returned status: " + status + " ; Message: " + responseBody);
        }

        return responseBody;
    }
}
