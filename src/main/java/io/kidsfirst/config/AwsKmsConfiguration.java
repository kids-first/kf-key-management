package io.kidsfirst.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;

@Configuration
@Profile("!dev | localstack")
public class AwsKmsConfiguration {

    @Bean
    public KmsAsyncClient awsKmsClient(
            @Value("${application.kms.endpoint:#{null}}") String endpoint,
            @Value("${aws.region:#{null}}") String region,
            @Value("${aws.accessKey:#{null}}") String accessKey,
            @Value("${aws.secretKey:#{null}}") String secretKey
    ) {
        KmsAsyncClientBuilder builder = KmsAsyncClient.builder();
        if (region != null) {
            builder.region(Region.of(region));
        }
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        if (accessKey != null) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }
}
