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

package io.kidsfirst.fence.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.kidsfirst.fence.model.FenceToken;
import io.kidsfirst.keys.core.manager.DynamoDBManager;
import static io.kidsfirst.fence.Constants.ENV_TOKEN_TABLE_NAME;
import static io.kidsfirst.fence.Constants.FIELD_NAME_OF_USER_ID_IN_EGO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FenceTokenDao {

  private static final DynamoDBMapper mapper = DynamoDBManager.mapper();

  private static volatile FenceTokenDao instance;

  // Specify the secret-table name as override to pass to mapper
  // FIXME: Deprecated method call, need to refactor to mapper definition out of manager
  private static final DynamoDBMapperConfig mapperConfigOverride = new DynamoDBMapperConfig(new TableNameOverride(System.getenv(ENV_TOKEN_TABLE_NAME)));
  private FenceTokenDao() { }

  public static FenceTokenDao instance() {

    if (instance == null) {
      synchronized(FenceTokenDao.class) {
        if (instance == null)
          instance = new FenceTokenDao();
      }
    }
    return instance;
  }

  public static List<FenceToken> getSecret(String userId) {

    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":val1", new AttributeValue().withS(userId));

    DynamoDBQueryExpression<FenceToken> queryExpression = new DynamoDBQueryExpression<FenceToken>()
      .withKeyConditionExpression(FIELD_NAME_OF_USER_ID_IN_EGO + " = :val1")
      .withExpressionAttributeValues(eav);

    return mapper.query(FenceToken.class, queryExpression, mapperConfigOverride);
  }

  public static void deleteSecret(String userId) {
    List<FenceToken> matchingSecrets =  getSecret(userId);
    matchingSecrets.forEach(match->mapper.delete(match, mapperConfigOverride));
  }

  public static void saveOrUpdateSecret(FenceToken secret) {
    mapper.save(secret, mapperConfigOverride);
  }

}
