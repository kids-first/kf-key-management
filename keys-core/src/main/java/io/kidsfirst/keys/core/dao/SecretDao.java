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

package io.kidsfirst.keys.core.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.kidsfirst.keys.core.manager.DynamoDBManager;
import io.kidsfirst.keys.core.model.Secret;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretDao {

  private static final DynamoDBMapper mapper = DynamoDBManager.mapper();

  private static volatile SecretDao instance;

  // Specify the secret-table name as override to pass to mapper
  // FIXME: Deprecated method call, need to refactor to mapper definition out of manager
  private static final DynamoDBMapperConfig mapperConfigOverride = new DynamoDBMapperConfig(new TableNameOverride(System.getenv("secret_table")));
  private SecretDao() { }

  public static SecretDao instance() {

    if (instance == null) {
      synchronized(SecretDao.class) {
        if (instance == null)
          instance = new SecretDao();
      }
    }
    return instance;
  }

  public static List<Secret> findAllSecrets() {
    return mapper.scan(Secret.class, new DynamoDBScanExpression(), mapperConfigOverride);
  }

  public static List<Secret> getSecret(String service, String userId) {

    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":val1", new AttributeValue().withS(userId));
    eav.put(":val2", new AttributeValue().withS(service));

    DynamoDBQueryExpression<Secret> queryExpression = new DynamoDBQueryExpression<Secret>()
      .withKeyConditionExpression("userId = :val1 and service = :val2")
      .withExpressionAttributeValues(eav);

    return mapper.query(Secret.class, queryExpression, mapperConfigOverride);
  }

  public static void deleteSecret(String service, String userId) {
    List<Secret> matchingSecrets =  getSecret(service, userId);
    matchingSecrets.forEach(match->mapper.delete(match, mapperConfigOverride));
  }

  public static void saveOrUpdateSecret(Secret secret) {
    mapper.save(secret, mapperConfigOverride);
  }

}