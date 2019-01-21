package io.kidsfirst.keys.core.utils;

import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.Secret;

import java.util.List;

public class FenceUtils {

  public enum Provider {
    DCF,
    GEN3;

    public String accessTokenKey() {
      return String.format("fence_%s_access",this.name()).toLowerCase();
    }

    public String refreshTokenKey() {
      return String.format("fence_%s_refresh",this.name()).toLowerCase();
    }
  }

  public static Provider getProvider(final String key) throws IllegalArgumentException {
    return Provider.valueOf(key.toUpperCase());

  }

  public static String getAccessToken(final Provider fence, final String userId) {
    List<Secret> secrets = SecretDao.getSecret(fence.accessTokenKey(), userId);
    if (!secrets.isEmpty()) {
      String token = secrets.get(0).getSecret();
      return KMSUtils.decrypt(token);

    }

    return "";
  }

  public static String getRefreshToken(final Provider fence, final String userId) {
    List<Secret> secrets = SecretDao.getSecret(fence.refreshTokenKey(), userId);
    if (!secrets.isEmpty()) {
      String token = secrets.get(0).getSecret();
      return KMSUtils.decrypt(token);

    }

    return "";
  }
}
