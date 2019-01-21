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

package io.kidsfirst.keys.get;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.model.Secret;
import lombok.var;

import java.net.HttpURLConnection;
import java.util.List;

public class GetSecret extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException {

    String userId = request.getUserId();

    String service = request.getQueryStringParameters().getOrDefault("service", null);
    if (service.isEmpty()) {
      throw new IllegalArgumentException("Required Parameter 'service' missing from URL Query.");
    }

    var resp = new LambdaResponse();
    resp.addDefaultHeaders();
    resp.addContentTypeHeader("text/plain");

    List<Secret> allSecrets = SecretDao.getSecret(service, userId);

    if (!allSecrets.isEmpty()) {
    Secret secret = allSecrets.get(0);
      // TODO: decrypt secretValue
      String secretValue = secret.getSecret();
      resp.setBody(secretValue);
      resp.setStatusCode(HttpURLConnection.HTTP_OK);

    } else {
      resp.setBody("");
      resp.setStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    }

    return resp;
  }

}