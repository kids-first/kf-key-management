package io.kidsfirst.keys.core.utils;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.keys.core.Constants;
import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.Secret;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.Optional;

public class FenceUtils {

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

    public String getClientId()     { return System.getenv(String.format("%s_%s", Constants.ENV_FENCE_CLIENT_ID,     this.name().toLowerCase())); }
    public String getClientSecret() { return System.getenv(String.format("%s_%s", Constants.ENV_FENCE_CLIENT_SECRET, this.name().toLowerCase())); }
    public String getEndpoint()     { return System.getenv(String.format("%s_%s", Constants.ENV_FENCE_ENDPOINT,      this.name().toLowerCase())); }
    public String getRedirectUri()  { return System.getenv(String.format("%s_%s", Constants.ENV_FENCE_REDIRECT_URI,  this.name().toLowerCase())); }
    public String getScope()        { return System.getenv(String.format("%s_%s", Constants.ENV_FENCE_SCOPE,         this.name().toLowerCase())); }
  }

  public static Provider getProvider(final String key) throws IllegalArgumentException {
    try {
      return Provider.valueOf(key.toUpperCase());

    } catch (IllegalArgumentException e) {
      // Override default message for unknown enum constant
      throw new IllegalArgumentException(String.format("Unknown fence identifier: %s", key), e);
    }

  }

  /* AuthClient POJO
  * Data object for passing auth client variables from env config
  * */

  @Data
  @NoArgsConstructor
  public static class AuthClient {
    String clientId;
    String clientSecret;
    String redirectUri;
    String scope;
  }

  public static AuthClient getAuthClient(final Provider fence) {
    val output = new AuthClient();

    output.setClientId(     fence.getClientId()     );
    output.setClientSecret( fence.getClientSecret() );
    output.setRedirectUri(  fence.getRedirectUri()  );
    output.setScope(        fence.getScope()        );

    return output;
  }

  public static Optional<String> fetchAccessToken(final Provider fence, final String userId) {
    return SecretUtils.fetchAndDecrypt(userId, fence.keyAccessToken());

  }

  public static void persistAccessToken(final Provider fence, final String userId, final String token) {
    val secret = new Secret(userId, fence.keyAccessToken(), token);
    SecretUtils.encryptAndSave(secret);

  }

  public static Optional<String> fetchRefreshToken(final Provider fence, final String userId) {
    return SecretUtils.fetchAndDecrypt(userId, fence.keyRefreshToken());

  }

  public static void persistRefreshToken(final Provider fence, final String userId, final String token) {
    val secret = new Secret(userId, fence.keyRefreshToken(), token);
    SecretUtils.encryptAndSave(secret);

  }

  public static void persistFenceUserId(final Provider fence, final String userId, final String token) {
    val secret = new Secret(userId, fence.keyUserId(), token);
    SecretUtils.encryptAndSave(secret);

  }

  public static void removeFenceTokens(final Provider fence, final String userId) {
    SecretDao.deleteSecret(fence.keyAccessToken(), userId);
    SecretDao.deleteSecret(fence.keyRefreshToken(), userId);
    SecretDao.deleteSecret(fence.keyUserId(), userId);
  }

  public static void persistTokens(final Provider fence, final String userId, final OIDCTokens tokens) {

    val fenceId = tokens.getIDTokenString();
    val accessToken = tokens.getAccessToken().getValue();
    val refreshToken = tokens.getRefreshToken().getValue();

    persistFenceUserId(fence, userId, fenceId);
    persistAccessToken(fence, userId, accessToken);
    persistRefreshToken(fence, userId, refreshToken);

  }

}
