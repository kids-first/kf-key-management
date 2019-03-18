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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class LambdaResponse {

  private int statusCode;
  private Map<String, String> headers = new HashMap<>();
  private String body;

  public void addCorsHeader() {
    String allowedDomains = System.getenv("corsAllowedDomains");
    if (allowedDomains == null || allowedDomains.isEmpty()) {
      allowedDomains = "*";
    }
    headers.put("Access-Control-Allow-Origin", allowedDomains);
  }

  public void addContentTypeHeader(final String contentType) {
    headers.put("Content-Type", contentType);
  }

  public void addDefaultHeaders() {
    this.addCorsHeader();
    this.addContentTypeHeader("application/json");

  }
}
