package io.kidsfirst.core.service;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.core.dao.SecretDao;
import io.kidsfirst.core.model.Provider;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.KMSService;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SecretService {

  private SecretDao secretDao;
  private KMSService kmsService;

  public SecretService(SecretDao secretDao, KMSService kmsService) {
    this.secretDao = secretDao;
    this.kmsService = kmsService;
  }

  public List<Secret> getSecret(String service, String userId) {
    return secretDao.getSecret(service, userId);
  }

  public void deleteSecret(String service, String userId) {
    secretDao.deleteSecret(service, userId);
  }

  public Optional<String> fetchAccessToken(final Provider fence, final String userId) {
    return fetchAndDecrypt(userId, fence.keyAccessToken());
  }

  public Optional<String> fetchRefreshToken(final Provider fence, final String userId) {
    return fetchAndDecrypt(userId, fence.keyRefreshToken());
  }

  public void persistAccessToken(final Provider fence, final String userId, final String token) {
    val secret = new Secret(userId, fence.keyAccessToken(), token);
    encryptAndSave(secret);
  }

  public void persistRefreshToken(final Provider fence, final String userId, final String token) {
    val secret = new Secret(userId, fence.keyRefreshToken(), token);
    encryptAndSave(secret);
  }

  public void persistFenceUserId(final Provider fence, final String userId, final String token) {
    val secret = new Secret(userId, fence.keyUserId(), token);
    encryptAndSave(secret);
  }

  public void removeFenceTokens(final Provider fence, final String userId) {
    deleteSecret(fence.keyAccessToken(), userId);
    deleteSecret(fence.keyRefreshToken(), userId);
    deleteSecret(fence.keyUserId(), userId);
  }

  public void persistTokens(final Provider fence, final String userId, final OIDCTokens tokens) {
    val fenceId = tokens.getIDTokenString();
    val accessToken = tokens.getAccessToken().getValue();
    val refreshToken = tokens.getRefreshToken().getValue();

    persistFenceUserId(fence, userId, fenceId);
    persistAccessToken(fence, userId, accessToken);
    persistRefreshToken(fence, userId, refreshToken);
  }

  public void encryptAndSave(final Secret secret) {
    val secretValue = secret.getSecret();
    val encryptedValue = kmsService.encrypt(secretValue);

    val encryptedSecret = new Secret(secret.getUserId(), secret.getService(), encryptedValue);

    secretDao.saveOrUpdateSecret(encryptedSecret);
  }

  public Optional<String> fetchAndDecrypt(final String userId, final String service) {
    val allSecrets = secretDao.getSecret(service, userId);

    if (!allSecrets.isEmpty()) {
      val secret = allSecrets.get(0);
      val secretValue = secret.getSecret();
      val decryptedValue = kmsService.decrypt(secretValue);

      return Optional.of(decryptedValue);
    } else {
      return Optional.empty();
    }

  }
}
