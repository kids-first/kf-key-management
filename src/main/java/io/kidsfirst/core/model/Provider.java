package io.kidsfirst.core.model;

import io.kidsfirst.core.service.FenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.EnumSet;

/* Provider Enum
 * list of configured fence endpoints
 * */
public enum Provider {
    // List of supported Fence Providers
    // - expected to match client provided query parameter (ignoreCase) unless you overwrite name()
    DCF,
    GEN3;
    public String keyAccessToken()  { return String.format("fence_%s_access",  this.name()).toLowerCase(); }
    public String keyRefreshToken() { return String.format("fence_%s_refresh", this.name()).toLowerCase(); }
    public String keyUserId()       { return String.format("fence_%s_user",    this.name()).toLowerCase(); }

    public String getClientId()     { return env.getProperty(this.getApplicationEnvKey("client_id"), env.getProperty(this.getEnvKey("fence_client_id"))); }
    public String getClientSecret() { return env.getProperty(this.getApplicationEnvKey("client_secret"), env.getProperty(this.getEnvKey("fence_client_secret"))); }
    public String getEndpoint()     { return env.getProperty(this.getApplicationEnvKey("token_endpoint"), env.getProperty(this.getEnvKey("fence_token_endpoint"))); }
    public String getRedirectUri()  { return env.getProperty(this.getApplicationEnvKey("redirect_uri"), env.getProperty(this.getEnvKey("fence_redirect_uri"))); }
    public String getScope()        { return env.getProperty(this.getApplicationEnvKey("scope"), env.getProperty(this.getEnvKey("fence_scope"))); }

    private String getEnvKey(String key) { return String.format("%s_%s", key, this.name().toLowerCase()); }
    private String getApplicationEnvKey(String key) { return String.format("application.fence.%s.%s", this.name().toLowerCase(), key); }
            
    @Component
    public static class ProviderServiceInjector {
        @Autowired
        private Environment env;

        @PostConstruct
        public void postConstruct() {
            for (Provider provider : EnumSet.allOf(Provider.class))
                provider.setEnv(env);
        }
    }

    private Environment env;

    private void setEnv(Environment env) {
        this.env = env;
    }
}
