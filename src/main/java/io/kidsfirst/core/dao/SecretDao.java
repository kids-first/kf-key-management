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

package io.kidsfirst.core.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.kidsfirst.core.manager.DynamoDBManager;
import io.kidsfirst.core.model.Secret;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecretDao {

  private final DynamoDBManager dynamoDBManager;
  private final Environment env;
  private final DynamoDBMapperConfig mapperConfigOverride;

  private SecretDao(DynamoDBManager dynamoDBManager, Environment env){
    this.dynamoDBManager = dynamoDBManager;
    this.env = env;

    String secretTableName = this.env.getProperty("application.secret_table", env.getProperty("secret_table"));
    if(secretTableName == null){
      throw new RuntimeException("secret_table not defined");
    }

    mapperConfigOverride = (new DynamoDBMapperConfig.Builder())
            .withTableNameOverride(new TableNameOverride(secretTableName))
            .withConversionSchema(DynamoDBMapperConfig.DEFAULT.getConversionSchema())
            .withBatchWriteRetryStrategy(DynamoDBMapperConfig.DEFAULT.getBatchWriteRetryStrategy())
            .withBatchLoadRetryStrategy(DynamoDBMapperConfig.DEFAULT.getBatchLoadRetryStrategy())
            .withSaveBehavior(null)
            .withConsistentReads(null)
            .withTableNameResolver(null)
            .withObjectTableNameResolver(null)
            .withPaginationLoadingStrategy(null)
            .withRequestMetricCollector(null)
            .build();
  }

  public List<Secret> getSecret(String service, String userId) {

    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":val1", new AttributeValue().withS(userId));
    eav.put(":val2", new AttributeValue().withS(service));

    DynamoDBQueryExpression<Secret> queryExpression = new DynamoDBQueryExpression<Secret>()
      .withKeyConditionExpression("userId = :val1 and service = :val2")
      .withExpressionAttributeValues(eav);

    return dynamoDBManager.query(Secret.class, queryExpression, mapperConfigOverride);
  }

  public void deleteSecret(String service, String userId) {
    List<Secret> matchingSecrets =  getSecret(service, userId);
    matchingSecrets.forEach(match-> dynamoDBManager.delete(match, mapperConfigOverride));
  }

  public void saveOrUpdateSecret(Secret secret) {
    dynamoDBManager.save(secret, mapperConfigOverride);
  }

}