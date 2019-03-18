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
import io.kidsfirst.keys.core.exception.NotFoundException;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.utils.SecretUtils;
import lombok.val;

import java.net.HttpURLConnection;

public class GetSecret extends LambdaRequestHandler {

  @Override
  public LambdaResponse processEvent(final LambdaRequest request)
      throws IllegalAccessException, IllegalArgumentException, NotFoundException
  {

    val userId = request.getUserId();

    val service = request.getQueryStringValue("service");

    val secretValue = SecretUtils.fetchAndDecrypt(userId, service);


    val resp = new LambdaResponse();
    resp.addDefaultHeaders();
    resp.addContentTypeHeader("text/plain");

    if (secretValue.isPresent()) {
      resp.setBody(secretValue.get());
      resp.setStatusCode(HttpURLConnection.HTTP_OK);

    } else {
      throw new NotFoundException(String.format("No value found for: %s", service));

    }

    return resp;
  }

}