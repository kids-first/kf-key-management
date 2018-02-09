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

package io.kidsfirst.keys.core.model;

import org.json.simple.JSONObject;

import java.util.Map;

public class LambdaResponse {

  private boolean isBase64Encoded;
  private String statusCode;
  private Map<String, String> headers;
  private String body;

  public LambdaResponse() {
  }

  public LambdaResponse(boolean isBase64Encoded, String statusCode, Map<String, String> headers, String body) {
    this.isBase64Encoded = isBase64Encoded;
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
  }

  public boolean isBase64Encoded() {
    return isBase64Encoded;
  }

  public void setBase64Encoded(boolean base64Encoded) {
    isBase64Encoded = base64Encoded;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setBody(JSONObject body) {
    this.body = body.toJSONString();
  }

  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("isBase64Encoded", isBase64Encoded);
    json.put("statusCode", statusCode);
    json.put("headers", new JSONObject(headers));
    json.put("body", body);

    return json;
  }

}
