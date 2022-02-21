package io.kidsfirst.core.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.io.Serializable;

@DynamoDbBean
public class Secret implements Serializable {


    private String userId;

    private String service;

    private String secret;

    public Secret() {
    }

    public Secret(String userId, String service, String secret) {
        this.userId = userId;
        this.service = service;
        this.secret = secret;
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

}
