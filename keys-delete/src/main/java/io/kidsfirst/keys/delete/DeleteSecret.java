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

package io.kidsfirst.keys.delete;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import lombok.var;

import java.net.HttpURLConnection;


public class DeleteSecret extends LambdaRequestHandler {

  /**
   * Remove secret for a given service and user
   */
  @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException {

    String userId = request.getUserId();

    var resp = new LambdaResponse();
    resp.addDefaultHeaders();

    String service = request.getBodyString("service");

    SecretDao.deleteSecret(service, userId);

    resp.setStatusCode(HttpURLConnection.HTTP_OK);

    return resp;
  }

}