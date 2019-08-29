/*
 * Copyright 2018 Ontario Institute for Cancer Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kidsfirst.keys.cavatica.proxy;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.model.Secret;
import io.kidsfirst.keys.core.utils.KMSUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.json.simple.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CavaticaProxy extends LambdaRequestHandler {

  static final String cavaticaApiRoot = System.getenv("cavatica_root");

  private static int[] HTTP_SUCCESS_CODES = { HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED,
      HttpURLConnection.HTTP_ACCEPTED, HttpURLConnection.HTTP_NO_CONTENT, HttpURLConnection.HTTP_RESET };
  private static String[] HTTP_ALLOWED_METHODS = { "GET", "POST", "PUT", "PATCH", "DELETE" };

  @Override
  public LambdaResponse processEvent(final LambdaRequest request)
      throws IllegalAccessException, IllegalArgumentException, IOException {

    String userId = request.getUserId();

    LambdaResponse resp = new LambdaResponse();
    resp.addDefaultHeaders();

    String cavaticaKey = getCavaticaKey(userId);

    // Path
    String path = request.getBodyString("path");

    // Method - confirm it is in HTTP_ALLOWED_METHODS or throw error
    String method = request.getBodyString("method").toUpperCase();
    if (Arrays.stream(HTTP_ALLOWED_METHODS).noneMatch(allowed -> allowed.equals(method))) {
      // Invalid method provided
      throw new IllegalArgumentException(String.format("Provided method '%s' is not allowed.", method));
    }

    // Body - default to null, get body content if applicable
    JSONObject body = null;
    if (Arrays.stream(new String[] { "POST", "PUT", "PATCH" }).anyMatch(m -> m.equals(method))) {
      body = (JSONObject) request.getBodyValue("body");
    }

    String bodyString = body == null ? null : body.toJSONString();

    String cavaticaResponse = sendCavaticaRequest(cavaticaKey, path, method, bodyString);

    resp.setBody(cavaticaResponse);
    resp.setStatusCode(HttpURLConnection.HTTP_OK);

    return resp;

  }

  private String getCavaticaKey(String userId) throws IllegalArgumentException {
    List<Secret> allSecrets = SecretDao.getSecret("cavatica", userId);

    if (!allSecrets.isEmpty()) {
      Secret secret = allSecrets.get(0);
      String secretValue = secret.getSecret();

      return KMSUtils.decrypt(secretValue);

    } else {
      throw new IllegalArgumentException("No Cavatica token available; unable to send request.");
    }
  }

  private String sendCavaticaRequest(String cavaticaKey, String path, String method, String body) throws IOException {

    URL url = new URL(cavaticaApiRoot + path);

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