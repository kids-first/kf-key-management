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

package io.kidsfirst.keys.put;

import io.kidsfirst.keys.core.LambdaRequestHandler;
import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.LambdaRequest;
import io.kidsfirst.keys.core.model.LambdaResponse;
import io.kidsfirst.keys.core.model.Secret;
import io.kidsfirst.keys.core.utils.KMSUtils;
import lombok.var;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class PutSecret extends LambdaRequestHandler{

  /**
   * Create secret object from userId and body of the Lambda event, encrypt the secretValue, and save to DynamoDB
   */
  @Override
  public LambdaResponse processEvent(final LambdaRequest request) throws IllegalAccessException, IllegalArgumentException {

    var resp = new LambdaResponse();
    resp.addDefaultHeaders();

    String userId = request.getUserId();

    // === 1. Create a Secret to hold the data to save
    Secret secret = new Secret();
    secret.setUserId(userId);

    // === 2. Get service and secretValue from event
    String service = request.getBodyValue("service");
    String secretValue = request.getBodyValue("secret");

    // === 3. Encrypt the secret value
    String encryptedSecret = KMSUtils.encrypt(secretValue);

    secret.setService(service);
    secret.setSecret(encryptedSecret);


    // === 4. Save to dynamo DB
    SecretDao.saveOrUpdateSecret(secret);
    return resp;
  }

}