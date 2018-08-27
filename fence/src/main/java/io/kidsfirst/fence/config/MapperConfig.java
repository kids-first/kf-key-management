package io.kidsfirst.fence.config;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import lombok.val;

import static io.kidsfirst.fence.Constants.ENV_TOKEN_TABLE_NAME;

public class MapperConfig {

  public static DynamoDBMapperConfig createConfig() {
    val config = DynamoDBMapperConfig.builder();
    config.setTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(System.getenv(ENV_TOKEN_TABLE_NAME)));
    config.setConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT);
    return config.build();
  }

}
