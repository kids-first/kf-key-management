package io.kidsfirst.fence;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import io.kidsfirst.keys.core.utils.KMSUtils;
import lombok.val;

import java.util.Optional;

import static io.kidsfirst.fence.Constants.*;

public class Utils {

    public static DynamoDB getDynamoDB() {
        return DynamoDBHolder.db;
    }

    private static class DynamoDBHolder {
        static final DynamoDB db = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build());
    }

    public static AuthorizationClient getAuthClient() {
        return AuthorizationClientHolder.instance;
    }

    private static class AuthorizationClientHolder {
        static final AuthorizationClient instance = computeValue();
        static AuthorizationClient computeValue() {

            val outcome = getDynamoDB().batchGetItem(
                    new TableKeysAndAttributes(geAuthorizationClientTableName()).withPrimaryKeys(new PrimaryKey(FIELD_NAME_OF_OPENID_PROVIDER, getOpenIdProvider())).withAttributeNames(FIELD_NAME_OF_CLIENT_ID, FIELD_NAME_OF_REDIRECT_URI, FIELD_NAME_OF_SCOPE, FIELD_NAME_OF_CLIENT_SECRET)
            );

            return
                outcome.getTableItems().get(geAuthorizationClientTableName()).stream().reduce(
                        new AuthorizationClient(),
                        (ac, item) -> {
                            ac.setClientId(Optional.of( item.getString(FIELD_NAME_OF_CLIENT_ID) ).orElse(null));
                            ac.setClientSecret(Optional.of(item.getString(FIELD_NAME_OF_CLIENT_SECRET)).orElse(null));
                            ac.setRedirectUri(Optional.of(item.getString(FIELD_NAME_OF_REDIRECT_URI)).orElse(null));
                            ac.setScope(Optional.of(item.getString(FIELD_NAME_OF_SCOPE)).orElse(null));

                            return ac;
                        },
                        (l, r) -> null
                );
        }
    }

    private static String getOpenIdProvider() {
        return DEFAULT_OPENID_PROVIDER;
    }

    private static class AuthorizationClienTableNameHolder{
        static final String tableName = computeValue();
        static String computeValue() {
            return Optional.ofNullable(System.getProperty(ENV_AUTH_CLIENT_TABLE_NAME)).orElse(DEFAULT_CLIENT_INFO_TABLE_NAME);
        }
    }

    private static String geAuthorizationClientTableName(){
        return AuthorizationClienTableNameHolder.tableName;
    }

    private static class TokenTableNameHolder{
        static final String tableName = computeValue();
        static String computeValue() {
            return Optional.ofNullable(System.getProperty(ENV_TOKEN_TABLE_NAME)).orElse(DEFAULT_TOKEN_TABLE_NAME);
        }
    }

    private static String getTokenTableName() {
        return TokenTableNameHolder.tableName;
    }

    public static void persistTokens(String userid_in_fence, String userid_in_ego, String access_token, String refresh_token) {

        getDynamoDB().batchWriteItem(
                new TableWriteItems(getTokenTableName())
                        .withItemsToPut(
                                new Item()
                                        .withPrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego)
                                        .withString(FIELD_NAME_OF_USER_ID_IN_FENCE, userid_in_fence)
                                        .withString(FIELD_NAME_OF_ACCESS_TOKEN, encrypt(access_token))
                                        .withString(FIELD_NAME_OF_REFRESH_TOKEN, encrypt(refresh_token))
                        )
        );
    }

    public static void updateTokens(String userid_in_ego, String access_token, String refresh_token) {

        getDynamoDB().getTable(getTokenTableName()).updateItem(
                new UpdateItemSpec()
                        .withPrimaryKey(new PrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego))
                        .withAttributeUpdate(new AttributeUpdate(FIELD_NAME_OF_ACCESS_TOKEN).put(encrypt(access_token)))
        );

        getDynamoDB().getTable(getTokenTableName()).updateItem(
                new UpdateItemSpec()
                        .withPrimaryKey(new PrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego))
                        .withAttributeUpdate(new AttributeUpdate(FIELD_NAME_OF_REFRESH_TOKEN).put(encrypt(refresh_token)))
        );
    }

    public static KfTokens retrieveTokens(String userid_in_ego) {
        val outcome =
                getDynamoDB().batchGetItem(
                        new TableKeysAndAttributes(getTokenTableName()).withPrimaryKeys(new PrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego)).withAttributeNames(FIELD_NAME_OF_USER_ID_IN_FENCE, FIELD_NAME_OF_ACCESS_TOKEN, FIELD_NAME_OF_REFRESH_TOKEN)
                );

        val tokens = new KfTokens();
        tokens.setUserid_in_ego(userid_in_ego);
        return
            outcome.getTableItems().get(getTokenTableName()).stream().reduce(
                    tokens,
                    (t, item) -> {
                        t.setUserid_in_fence(Optional.of(item.getString(FIELD_NAME_OF_USER_ID_IN_FENCE)).orElse(null));
                        t.setAccess_token(decrypt(Optional.of(item.getString(FIELD_NAME_OF_ACCESS_TOKEN)).orElse(null)));
                        t.setRefresh_token(decrypt(Optional.of(item.getString(FIELD_NAME_OF_REFRESH_TOKEN)).orElse(null)));
                        return t;
                    },
                    (l, r) -> null
            );
    }

    private static String encrypt(String token) {
        return KMSUtils.encrypt(token);
    }

    private static String decrypt(String cipher) {
        return KMSUtils.decrypt(cipher);
    }

}
