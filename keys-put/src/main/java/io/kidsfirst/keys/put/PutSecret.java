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
import io.kidsfirst.keys.core.model.Secret;
import io.kidsfirst.keys.core.utils.KMSUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class PutSecret extends LambdaRequestHandler{

  /**
   * Create secret object from userId and body of the Lambda event, encrypt the secretValue, and save to DynamoDB
   */
  @Override
  public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException {

    // === 1. Create a Secret to hold our data
    Secret secret = new Secret();

    if (userId == null) {
      throw new IllegalArgumentException("User ID not found.");
    }
    secret.setUserId(userId);

    try {

      // === 2. Get service and secretValue from body
      JSONParser parser = new JSONParser();
      JSONObject body = (JSONObject) parser.parse((String) event.get("body"));

      String service = (String) body.get("service");
      String secretValue = (String) body.get("secret");

      if (service == null) {
        throw new IllegalArgumentException("Required Field [service] missing in request body.");
      }

      if (secretValue == null) {
        throw new IllegalArgumentException("Required Field [secret] missing in request body.");
      }

      // === 3. Encrypt the secret value
      String encryptedSecret = KMSUtils.encrypt(secretValue);

      secret.setService(service);
      secret.setSecret(encryptedSecret);

    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Exception thrown accessing request data: " + e.getMessage());

    }

    // === 4. Save to dynamo DB
    SecretDao.saveOrUpdateSecret(secret);
    return "";
  }

}