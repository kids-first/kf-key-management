package io.kidsfirst.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDbConfig {


    private final String dynamoDbEndPointUrl;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public DynamoDbConfig(@Value("${aws.dynamodb.endpoint}") String dynamoDbEndPointUrl,
                          @Value("${aws.region}") String region,
                          @Value("${aws.accessKey}") String accessKey,
                          @Value("${aws.secretKey}") String secretKey) {
        this.dynamoDbEndPointUrl = dynamoDbEndPointUrl;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }


    @Bean
    public DynamoDbAsyncClient getDynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
        if (region != null) {
            builder.region(Region.of(region));
        }
        if (dynamoDbEndPointUrl != null) {
            builder
                    .endpointOverride(URI.create(dynamoDbEndPointUrl));
        }
        if (accessKey != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder
                .build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient getDynamoDbEnhancedAsyncClient() {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(getDynamoDbAsyncClient())
                .build();
    }

}