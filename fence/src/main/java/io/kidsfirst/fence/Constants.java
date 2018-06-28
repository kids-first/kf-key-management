package io.kidsfirst.fence;

public interface Constants {
    String ENV_CLIENT_INFO_TABLE_NAME = "KF_LAMBDA_AUTH_CLIENT_TABLE_NAME";
    String ENV_FENCE_TOKEN_ENDPOINT = "KF_LAMBDA_FENCE_TOKEN_ENDPOINT";
    String ENV_FENCE_PUBLIC_KEY = "KF_LAMBDA_FENCE_PUBLIC_KEY";
    String ENV_EGO_PUBLIC_KEY = "KF_LAMBDA_EGO_PUBLIC_KEY";
    String ENV_TOKEN_TABLE_NAME = "KF_LAMBDA_TOKEN_TABLE_NAME";
    String DEFAULT_CLIENT_INFO_TABLE_NAME = "kf_auth_client";
    String DEFAULT_TOKEN_TABLE_NAME = "kf_token";
    String FIELD_NAME_OF_CLIENT_ID = "client_id";
    String FIELD_NAME_OF_REDIRECT_URI = "redirect_uri";
    String FIELD_NAME_OF_SCOPE = "scope";
    String FIELD_NAME_OF_OPENID_PROVIDER = "openid_provider";
    String DEFAULT_OPENID_PROVIDER = "fence";
    String FIELD_NAME_OF_CLIENT_SECRET = "client_secret";
    String FIELD_NAME_OF_USER_ID_IN_EGO = "userid_in_ego";
    String FIELD_NAME_OF_USER_ID_IN_FENCE = "userid_in_fence";
    String FIELD_NAME_OF_ACCESS_TOKEN = "access_token";
    String FIELD_NAME_OF_REFRESH_TOKEN = "refresh_token";
    String FIELD_NAME_OF_ID_TOKEN = "id_token";
    String FIELD_NAME_OF_EGO_JWT = "EGO_JWT";
    
}
