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
import io.kidsfirst.keys.core.model.Secret;
import io.kidsfirst.keys.core.utils.KMSUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class DeleteSecret extends LambdaRequestHandler{

  /**
   * Remove secret for a given service and user
   */
  @Override
  public String processEvent(JSONObject event, String userId) throws IllegalArgumentException, ParseException {


    if (userId == null) {
      throw new IllegalArgumentException("User ID not found.");
    }

    try {

      // === 1. Get service from event
      JSONParser parser = new JSONParser();
      JSONObject body = (JSONObject) parser.parse((String) event.get("body"));

      String service = (String) body.get("service");

      if (service == null) {
        throw new IllegalArgumentException("Required Field [service] missing in request body.");
      }

      // === 2. Call Delete
      SecretDao.deleteSecret(service, userId);

    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Exception thrown accessing request data: " + e.getMessage());

    }


    return "";
  }

}