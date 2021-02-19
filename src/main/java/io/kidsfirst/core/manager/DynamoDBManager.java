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

package io.kidsfirst.core.manager;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DynamoDBManager extends DynamoDBMapper {

  private DynamoDBManager(Environment env) {
    super(env.getProperty("application.dynamodb.endpoint") == null ?
            AmazonDynamoDBClient.builder().build() :
            AmazonDynamoDBClient.builder().withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(env.getProperty("application.dynamodb.endpoint"), Regions.US_EAST_1.getName())
            ).build());
  }
}
