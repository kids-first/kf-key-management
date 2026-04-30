package io.kidsfirst.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
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

    @Getter
    @Setter
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

        public String keyAccessToken() {
            return String.format("fence_%s_access", this.name).toLowerCase();
        }

        public String keyRefreshToken() {
            return String.format("fence_%s_refresh", this.name).toLowerCase();
        }

        public String keyUserId() {
            return String.format("fence_%s_user", this.name).toLowerCase();
        }

        public boolean hasApi() {
            return apiEndpoint != null;
        }

        public boolean hasProxy() {
            return proxyUri != null;
        }

        public Integer getAccessTokenLifetimeBuffer() {
            return accessTokenLifetimeBuffer != null ? accessTokenLifetimeBuffer : 0;
        }
    }

}
