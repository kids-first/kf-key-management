package io.kidsfirst.keys.core.utils;

import io.kidsfirst.keys.core.dao.SecretDao;
import io.kidsfirst.keys.core.model.Secret;
import lombok.val;

import java.util.Optional;

public class SecretUtils {

  public static void encryptAndSave(final Secret secret) {
    val secretValue = secret.getSecret();
    val encryptedValue = KMSUtils.encrypt(secretValue);

    val encryptedSecret = new Secret(secret.getUserId(), secret.getService(), encryptedValue);

    SecretDao.saveOrUpdateSecret(encryptedSecret);
  }

  public static Optional<String> fetchAndDecrypt(final String userId, final String service) {
    val allSecrets = SecretDao.getSecret(service, userId);

    if (!allSecrets.isEmpty()) {
      val secret = allSecrets.get(0);
      val secretValue = secret.getSecret();
      val decryptedValue = KMSUtils.decrypt(secretValue);

      return Optional.of(decryptedValue);
    } else {
      return Optional.empty();
    }

  }
}
