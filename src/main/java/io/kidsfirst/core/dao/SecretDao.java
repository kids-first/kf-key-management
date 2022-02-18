package io.kidsfirst.core.dao;

import io.kidsfirst.core.model.Secret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.concurrent.CompletableFuture;

@Repository
public class SecretDao {

    private final DynamoDbAsyncTable<Secret> secretDynamoDbAsyncTable;

    private SecretDao(DynamoDbEnhancedAsyncClient enhancedAsyncClient, @Value("${application.secret_table}") String tableName) {
        this.secretDynamoDbAsyncTable = enhancedAsyncClient.table(tableName, TableSchema.fromBean(Secret.class));
    }

    public CompletableFuture<Secret> getSecret(String service, String userId) {
        return secretDynamoDbAsyncTable.getItem(getKeyBuild(service, userId));
    }

    public CompletableFuture<Secret> deleteSecret(String service, String userId) {
        return secretDynamoDbAsyncTable.deleteItem(getKeyBuild(service, userId));
    }

    private Key getKeyBuild(String service, String userId) {
        return Key.builder().partitionValue(userId).sortValue(service).build();
    }

    public CompletableFuture<Secret> saveOrUpdateSecret(Secret secret) {
        return secretDynamoDbAsyncTable.updateItem(secret);
    }

}