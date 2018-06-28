package io.kidsfirst.fence;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.nimbusds.oauth2.sdk.token.Tokens;
import lombok.val;

import java.util.Optional;

import static io.kidsfirst.fence.Constants.*;

public class Utils {

    public static DynamoDB dynamo_db = DynamoDBHolder.db;

    private static class DynamoDBHolder {
        static final DynamoDB db = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build());
    }
    
    public static AuthorizationClient auth_client = AuthorizationClientHolder.instance;

    private static class AuthorizationClientHolder {
        static final AuthorizationClient instance = computeValue();
        static AuthorizationClient computeValue() {

            val outcome = dynamo_db.batchGetItem(
                    new TableKeysAndAttributes(auth_client_table_name).withPrimaryKeys(new PrimaryKey(FIELD_NAME_OF_OPENID_PROVIDER, openid_provider)).withAttributeNames(FIELD_NAME_OF_CLIENT_ID, FIELD_NAME_OF_REDIRECT_URI, FIELD_NAME_OF_SCOPE, FIELD_NAME_OF_CLIENT_SECRET)
            );

            return
                outcome.getTableItems().get(auth_client_table_name).stream().reduce(
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

    public static String auth_client_table_name = geAuthorizationClientTableName();

    public static String tokens_table_name = getTokensTableName();

    public static String openid_provider = getOpenIdProvider();

    private static String getOpenIdProvider() {
        return DEFAULT_OPENID_PROVIDER;
    }

    private static String geAuthorizationClientTableName(){
        return Optional.of(System.getProperty(ENV_CLIENT_INFO_TABLE_NAME)).orElse(DEFAULT_CLIENT_INFO_TABLE_NAME);
    }

    private static String getTokensTableName() {
        return Optional.of(System.getProperty(ENV_TOKEN_TABLE_NAME)).orElse(DEFAULT_TOKEN_TABLE_NAME);
    }

    public static void persistTokens(String userid_in_fence, String userid_in_ego, String access_token, String refresh_token) {

        dynamo_db.batchWriteItem(
                new TableWriteItems(tokens_table_name)
                        .withItemsToPut(
                                new Item()
                                        .withPrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego)
                                        .withString(FIELD_NAME_OF_USER_ID_IN_FENCE, userid_in_fence)
                                        .withString(FIELD_NAME_OF_ACCESS_TOKEN, access_token)
                                        .withString(FIELD_NAME_OF_REFRESH_TOKEN, refresh_token)
                        )
        );
    }

    public static void updateTokens(String userid_in_ego, String access_token, String refresh_token) {

        dynamo_db.getTable(tokens_table_name).updateItem(
                new UpdateItemSpec()
                        .withPrimaryKey(new PrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego))
                        .withAttributeUpdate(new AttributeUpdate(FIELD_NAME_OF_ACCESS_TOKEN).put(access_token))
                        .withAttributeUpdate(new AttributeUpdate(FIELD_NAME_OF_REFRESH_TOKEN).put(refresh_token))
        );
    }

    public static KfTokens retrieveTokens(String userid_in_ego) {
        val outcome =
                dynamo_db.batchGetItem(
                        new TableKeysAndAttributes(tokens_table_name).withPrimaryKeys(new PrimaryKey(FIELD_NAME_OF_USER_ID_IN_EGO, userid_in_ego)).withAttributeNames(FIELD_NAME_OF_USER_ID_IN_FENCE, FIELD_NAME_OF_ACCESS_TOKEN, FIELD_NAME_OF_REFRESH_TOKEN)
                );

        val tokens = new KfTokens();
        tokens.setUserid_in_ego(userid_in_ego);
        return
            outcome.getTableItems().get(tokens_table_name).stream().reduce(
                    tokens,
                    (t, item) -> {
                        t.setUserid_in_fence(Optional.of(item.getString(FIELD_NAME_OF_USER_ID_IN_FENCE)).orElse(null));
                        t.setAccess_token(Optional.of(item.getString(FIELD_NAME_OF_ACCESS_TOKEN)).orElse(null));
                        t.setRefresh_token(Optional.of(item.getString(FIELD_NAME_OF_REFRESH_TOKEN)).orElse(null));
                        return t;
                    },
                    (l, r) -> null
            );
    }

}
