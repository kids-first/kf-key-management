package io.kidsfirst.core.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.io.Serializable;
import java.time.Instant;

@DynamoDbBean
public class Secret implements Serializable {

    private String userId;

    private String service;

    private String secret;

    private Long expiration;

    public Secret() {
    }

    public Secret(String userId, String service, String secret, Long expiration) {
        this.userId = userId;
        this.service = service;
        this.secret = secret;
        this.expiration = expiration;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("service")
    public String getService() {
        return service;
    }

    public void setService(String type) {
        this.service = type;
    }

    @DynamoDbAttribute("secret")
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @DynamoDbAttribute("expiration")
    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    public boolean notExpired() {
        return expiration != null && Instant.now().getEpochSecond() < expiration;
    }
}
