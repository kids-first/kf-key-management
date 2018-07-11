# Fence Integration

There are 4 Lambda functions defined in this module

## GetTokens

* Environment Variables

    * `ego_public`: ego public key
    * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
    * `kms_enabled`: whether to enable kms encryption/decryption. If the variable is missing, by default disable kms functionanlity

* HTTP Header

    * Authorization: `Bearer ${ego_user_jwt_token}`
    
* Dynamo DB

    * The default table name is `kf_tokens`

* Return  
    `{"access_code": "...","refresh_code": "..."}`
    
    
## RequestAuthorizationClient

 * Environment Variables
 
     * `auth_client_table_name`: DynamoDB table name for auth client information, the default value is `kf_auth_client`
     
 * Dynamo DB
 
     * The default table name is `kf_auth_client`
      
 * Return  
     `{"client_id": "...","redirect_uri": "...", "scope":"..."}`
     
## RequestTokens

 * Environment Variables
 
     * `auth_client_table_name`: DynamoDB table name for auth client information, the default value is `kf_auth_client`
     * `ego_public`: ego public key
     * `fence_public`: fence public key
     * `fence_token_endpoint`: fence token endpoint, the default value is `https://gen3qa.kids-first.io/user/oauth2/token`
     * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
     * `kms_enabled`: whether to enable kms encryption/decryption. If the variable is missing, by default disable kms functionanlity
     
 * Dynamo DB
 
     * The default table name is `kf_tokens`
     
 * HTTP Header
 
     * Authorization: `Bearer ${ego_user_jwt_token}`
     
 * HTTP Query String Parameters
 
     * `code`: auth code

 * Return  
     `{"access_code": "...","refresh_code": "...", "id_token": "..."}`
     
## RefreshTokens

* Environment Variables
 
     * `auth_client_table_name`: DynamoDB table name for auth client information, the default value is `kf_auth_client`
     * `ego_public`: ego public key
     * `fence_public`: fence public key
     * `fence_token_endpoint`: fence token endpoint, the default value is `https://gen3qa.kids-first.io/user/oauth2/token`
     * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
     * `kms_enabled`: whether to enable kms encryption/decryption. If the variable is missing, by default disable kms functionanlity
     
 * Dynamo DB
 
     * The default table name is `kf_tokens`
     
 * HTTP Header
  
     * Authorization: `Bearer ${ego_user_jwt_token}`

 * Return  
     `{"access_code": "...","refresh_code": "..."}`