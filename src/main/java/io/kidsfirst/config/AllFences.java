package io.kidsfirst.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "application")
public class AllFences {

    private Map<String, Fence> fence;


    public void setFence(Map<String, Fence> fence) {
        this.fence = fence;
    }

    public Fence get(String key) {
        return fence.get(key);
    }

    public Collection<Fence> all() {
        return fence.values();
    }

    public static class Fence {


        private String clientId;
        private String clientSecret;
        private String tokenEndpoint;
        private String apiEndpoint;
        private String redirectUri;
        private String proxyUri;
        private String scope;
        private String name;
        private Integer refreshTokenLifetime;
        private Integer accessTokenLifetimeBuffer;
        private String authorizeUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public void setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String keyAccessToken() {
            return String.format("fence_%s_access", this.name).toLowerCase();
        }

        public String keyRefreshToken() {
            return String.format("fence_%s_refresh", this.name).toLowerCase();
        }

        public String keyUserId() {
            return String.format("fence_%s_user", this.name).toLowerCase();
        }

        public String getApiEndpoint() {
            return apiEndpoint;
        }

        public void setApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }

        public boolean hasApi() {
            return apiEndpoint != null;
        }

        public boolean hasProxy() {
            return proxyUri != null;
        }

        public String getProxyUri() {
            return proxyUri;
        }

        public void setProxyUri(String proxyUri) {
            this.proxyUri = proxyUri;
        }

        public Integer getRefreshTokenLifetime() {
            return refreshTokenLifetime;
        }

        public void setRefreshTokenLifetime(Integer refreshTokenLifetime) {
            this.refreshTokenLifetime = refreshTokenLifetime;
        }

        public Integer getAccessTokenLifetimeBuffer() {
            return accessTokenLifetimeBuffer != null ? accessTokenLifetimeBuffer : 0;
        }

        public void setAccessTokenLifetimeBuffer(Integer accessTokenLifetimeBuffer) {
            this.accessTokenLifetimeBuffer = accessTokenLifetimeBuffer;
        }

        public String getAuthorizeUri() {
            return authorizeUri;
        }

        public void setAuthorizeUri(String authorizeUri) {
            this.authorizeUri = authorizeUri;
        }
    }

}
