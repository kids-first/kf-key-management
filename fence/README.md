# Fence Integration

There are 4 Lambda functions defined in this module

## GetTokens

* Environment Variables

    * `ego_public`: ego public key
    * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
    * `kms_enabled`: whether to enable kms encryption/decryption. If the variable is missing, by default disable kms functionanlity
    * `kms`: KMS encryption key

* HTTP Header

    * Authorization: `Bearer ${ego_user_jwt_token}`
    
* Dynamo DB

    * The default table name is `kf_tokens`

* Return  
    `{"access_code": "...","refresh_code": "..."}`
    
    
## RequestAuthorizationClient

 * Environment Variables
 
     * `fence_client_id`: kids-first portal's client id assigned by FENCE
     * `fence_client_secret`: kids-first portal's client secret assigned by FENCE
     * `fence_redirect_uri`: kids-first portal's redirect_uri required by FENCE
     * `fence_scope`: kids-first portal's scope accepted by FENCE
     
 * Dynamo DB
 
     * The default table name is `kf_auth_client`
      
 * Return  
     `{"client_id": "...","redirect_uri": "...", "scope":"..."}`
     
## RequestTokens

 * Environment Variables
 
     * `ego_public`: ego public key
     * `fence_public`: fence public key
     * `fence_token_endpoint`: fence token endpoint, the default value is `https://gen3qa.kids-first.io/user/oauth2/token`
     * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
     * `kms_enabled`: whether to enable kms encryption/decryption. If the variable is missing, by default disable kms functionanlity
     * `kms`: KMS encryption key
     * `fence_client_id`: kids-first portal's client id assigned by FENCE
     * `fence_client_secret`: kids-first portal's client secret assigned by FENCE
     * `fence_redirect_uri`: kids-first portal's redirect_uri required by FENCE
     * `fence_scope`: kids-first portal's scope accepted by FENCE
     
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
 
     * `ego_public`: ego public key
     * `fence_public`: fence public key
     * `fence_token_endpoint`: fence token endpoint, the default value is `https://gen3qa.kids-first.io/user/oauth2/token`
     * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
     * `kms_enabled`: whether to enable kms encryption/decryption. If the variable is missing, by default disable kms functionanlity
     * `kms`: KMS encryption key    
     * `fence_client_id`: kids-first portal's client id assigned by FENCE
     * `fence_client_secret`: kids-first portal's client secret assigned by FENCE
     * `fence_redirect_uri`: kids-first portal's redirect_uri required by FENCE
     * `fence_scope`: kids-first portal's scope accepted by FENCE     
     
 * Dynamo DB
 
     * The default table name is `kf_tokens`
     
 * HTTP Header
  
     * Authorization: `Bearer ${ego_user_jwt_token}`

 * Return  
     `{"access_code": "...","refresh_code": "..."}`
     
## RemoveTokens

* Environment Variables
    
    * `ego_public`: ego public key
    * `token_table_name`: DynamoDB table name for tokens, the default value is `kf_tokens`
    
* Dynamo DB

    * The default table name is `kf_tokens`
 
* HTTP Header

    * Authorization: `Bearer ${ego_user_jwt_token}`

* Return  
    what ever AWS `DeleteItemOutcome` returns