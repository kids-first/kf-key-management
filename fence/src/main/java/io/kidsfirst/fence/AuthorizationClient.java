package io.kidsfirst.fence;

import lombok.Data;

@Data
public class AuthorizationClient {
    String clientId;
    String clientSecret;
    String redirectUri;
    String scope;
}
