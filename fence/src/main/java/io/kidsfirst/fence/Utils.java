package io.kidsfirst.fence;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import io.kidsfirst.fence.dao.FenceTokenDao;
import io.kidsfirst.fence.model.FenceToken;
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

            val ac =  new AuthorizationClient();
            ac.setClientId(System.getenv(ENV_FENCE_CLIENT_ID));
            ac.setClientSecret(System.getenv(ENV_FENCE_CLIENT_SECRET));
            ac.setRedirectUri(System.getenv(ENV_FENCE_REDIRECT_URI));
            ac.setScope(System.getenv(ENV_FENCE_SCOPE));
            return ac;
        }
    }

    private static class TokenTableNameHolder{
        static final String tableName = computeValue();
        static String computeValue() {
            return Optional.ofNullable(System.getenv(ENV_TOKEN_TABLE_NAME)).orElse(DEFAULT_TOKEN_TABLE_NAME);
        }
    }

    private static String getTokenTableName() {
        return TokenTableNameHolder.tableName;
    }

    private static class KmsEnabledHolder {
        static final Boolean kmsEnabled = computeValue();
        static Boolean computeValue() {
            String env = System.getenv(ENV_KMS_ENABLED);
            if(env == null){
                return true;
            }
            else{
                return Boolean.valueOf(env);
            }
        }
    }

    private static Boolean isKmsEnabled() {
        return KmsEnabledHolder.kmsEnabled;
    }

    public static void persistTokens(String userid_in_fence, String userid_in_ego, String access_token, String refresh_token) {
        FenceToken token = new FenceToken(userid_in_ego);
        token.setFenceUserId(userid_in_fence);
        token.setAccessToken(encrypt(access_token));
        token.setRefreshToken(encrypt(refresh_token));
        try {
            FenceTokenDao.saveOrUpdateSecret(token);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Exception thrown accessing request data: " + e.getMessage());
        }
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
                        t.setAccess_token(decrypt( Optional.of(item.getString(FIELD_NAME_OF_ACCESS_TOKEN)).orElse(null)));
                        t.setRefresh_token(decrypt( Optional.of(item.getString(FIELD_NAME_OF_REFRESH_TOKEN)).orElse(null)));
                        return t;
                    },
                    (l, r) -> null
            );
    }

    public static String removeTokens(String userid_in_ego) {

        val table = getDynamoDB().getTable(getTokenTableName());
        val deleteItemSpec = new DeleteItemSpec().withPrimaryKey(new PrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego));
        val outcome = getDynamoDB().getTable(getTokenTableName()).deleteItem(deleteItemSpec);
        return outcome.getDeleteItemResult().toString();
    }

    private static String encrypt(String token) {
        if(isKmsEnabled())
            return KMSUtils.encrypt(token);
        else
            return token;
    }

    private static String decrypt(String cipher) {
        if (isKmsEnabled())
            return KMSUtils.decrypt(cipher);
        else
            return cipher;
    }

}
