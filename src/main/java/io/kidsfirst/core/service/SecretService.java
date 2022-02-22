package io.kidsfirst.core.service;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.core.dao.SecretDao;
import io.kidsfirst.core.model.Provider;
import io.kidsfirst.core.model.Secret;
import lombok.val;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
public class SecretService {

    private final SecretDao secretDao;
    private final KMSService kmsService;

    public SecretService(SecretDao secretDao, KMSService kmsService) {
        this.secretDao = secretDao;
        this.kmsService = kmsService;
    }

    public Mono<Secret> getSecret(String service, String userId) {
        CompletableFuture<Secret> secret = secretDao.getSecret(service, userId);

        return Mono.fromFuture(secret);
    }

    public Mono<Secret> deleteSecret(String service, String userId) {
        return Mono.fromFuture(secretDao.deleteSecret(service, userId));
    }

    public Mono<String> fetchAccessToken(final Provider fence, final String userId) {
        return fetchAndDecrypt(userId, fence.keyAccessToken());
    }

    public Mono<String> fetchRefreshToken(final Provider fence, final String userId) {
        return fetchAndDecrypt(userId, fence.keyRefreshToken());
    }

    public Mono<Secret> persistAccessToken(final Provider fence, final String userId, final String token) {
        val secret = new Secret(userId, fence.keyAccessToken(), token);
        return encryptAndSave(secret);
    }

    public Mono<Secret> persistRefreshToken(final Provider fence, final String userId, final String token) {
        val secret = new Secret(userId, fence.keyRefreshToken(), token);
        return encryptAndSave(secret);
    }

    public Mono<Secret> persistFenceUserId(final Provider fence, final String userId, final String token) {
        val secret = new Secret(userId, fence.keyUserId(), token);
        return encryptAndSave(secret);
    }

    public Flux<Secret> removeFenceTokens(final Provider fence, final String userId) {
        return Flux.merge(deleteSecret(fence.keyAccessToken(), userId), deleteSecret(fence.keyRefreshToken(), userId), deleteSecret(fence.keyUserId(), userId));
    }

    public Flux<Secret> persistTokens(final Provider fence, final String userId, final OIDCTokens tokens) {
        val fenceId = tokens.getIDTokenString();
        val accessToken = tokens.getAccessToken().getValue();
        val refreshToken = tokens.getRefreshToken().getValue();

        return Flux.merge(persistFenceUserId(fence, userId, fenceId),
                persistAccessToken(fence, userId, accessToken),
                persistRefreshToken(fence, userId, refreshToken));
    }

    public Mono<Secret> encryptAndSave(final Secret secret) {
        val secretValue = secret.getSecret();
        val encryptedValue = kmsService.encrypt(secretValue);
        return encryptedValue
                .mapNotNull(s->new Secret(secret.getUserId(), secret.getService(), s))
                .flatMap(s-> Mono.fromFuture(secretDao.saveOrUpdateSecret(s)));
    }

    public Mono<String> fetchAndDecrypt(final String userId, final String service) {
        val secret = getSecret(service, userId);
        return secret.mapNotNull(s -> s).flatMap(s -> kmsService.decrypt(s.getSecret()));

    }


}
