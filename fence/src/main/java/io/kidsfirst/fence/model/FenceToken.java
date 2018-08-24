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

package io.kidsfirst.fence.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import lombok.Data;

import java.io.Serializable;

import static io.kidsfirst.fence.Constants.*;

@Data
public class FenceToken implements Serializable {

  @DynamoDBHashKey(attributeName = FIELD_NAME_OF_USER_ID_IN_EGO)
  private String userId;

  @DynamoDBAttribute(attributeName = FIELD_NAME_OF_USER_ID_IN_FENCE)
  private String fenceUserId;

  @DynamoDBAttribute(attributeName = FIELD_NAME_OF_ACCESS_TOKEN)
  private String accessToken;

  @DynamoDBAttribute(attributeName = FIELD_NAME_OF_REFRESH_TOKEN)
  private String refreshToken;

  public FenceToken() {}

  public FenceToken(String userId) {
    if(userId == null) {
      throw new IllegalArgumentException("User ID in Ego not found");
    }
    this.setUserId(userId);
  }

}
