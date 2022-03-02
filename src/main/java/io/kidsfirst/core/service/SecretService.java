package io.kidsfirst.core.service;

import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.config.AllFences;
import io.kidsfirst.core.dao.SecretDao;
import io.kidsfirst.core.model.Secret;
import lombok.val;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class SecretService {

    private final SecretDao secretDao;
    private final KmsService kmsService;

    public SecretService(SecretDao secretDao, KmsService kmsService) {
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

    public Mono<String> fetchAccessToken(final AllFences.Fence fence, final String userId) {
        return fetchAndDecrypt(userId, fence.keyAccessToken());
    }

    public Mono<String> fetchRefreshToken(final AllFences.Fence fence, final String userId) {
        return fetchAndDecrypt(userId, fence.keyRefreshToken());
    }

    public Mono<Secret> persistAccessToken(final AllFences.Fence fence, final String userId, final String token, Long expiration) {
        val secret = new Secret(userId, fence.keyAccessToken(), token, expiration);
        return encryptAndSave(secret);
    }

    public Mono<Secret> persistRefreshToken(final AllFences.Fence fence, final String userId, final String token, Long expiration) {
        //For refresh token, expiration date is set only the first time
        val existingSecret = Mono.fromFuture(secretDao.getSecret(fence.keyRefreshToken(), userId));
        return existingSecret.map(s -> s.getExpiration() != null ? s.getExpiration() : expiration).defaultIfEmpty(expiration)
                .flatMap(exp -> {
                    val secret = new Secret(userId, fence.keyRefreshToken(), token, exp);
                    return encryptAndSave(secret);
                });

    }

    public Mono<Secret> persistFenceUserId(final AllFences.Fence fence, final String userId, final String token, Long expiration) {
        val secret = new Secret(userId, fence.keyUserId(), token, expiration);
        return encryptAndSave(secret);
    }

    public Flux<Secret> removeFenceTokens(final AllFences.Fence fence, final String userId) {
        return Flux.merge(deleteSecret(fence.keyAccessToken(), userId), deleteSecret(fence.keyRefreshToken(), userId), deleteSecret(fence.keyUserId(), userId));
    }

    public Flux<Secret> persistTokens(final AllFences.Fence fence, final String userId, final OIDCTokens tokens) {
        val fenceId = tokens.getIDTokenString();
        val accessToken = tokens.getAccessToken().getValue();
        val refreshToken = tokens.getRefreshToken().getValue();

        val accessTokenExpiration = Instant.now()
                .plus(tokens.getAccessToken().getLifetime(), SECONDS)
                .minus(fence.getAccessTokenLifetimeBuffer(), SECONDS)
                .getEpochSecond();

        val refreshTokenExpiration = Instant.now()
                .plus(fence.getRefreshTokenLifetime(), SECONDS)
                .getEpochSecond();

        return Flux.merge(persistFenceUserId(fence, userId, fenceId, accessTokenExpiration),
                persistAccessToken(fence, userId, accessToken, accessTokenExpiration),
                persistRefreshToken(fence, userId, refreshToken, refreshTokenExpiration));
    }

    public Mono<Secret> encryptAndSave(final Secret secret) {
        val secretValue = secret.getSecret();
        val encryptedValue = kmsService.encrypt(secretValue);
        return encryptedValue
                .mapNotNull(s -> new Secret(secret.getUserId(), secret.getService(), s, secret.getExpiration()))
                .flatMap(s -> Mono.fromFuture(secretDao.saveOrUpdateSecret(s)));
    }

    public Mono<String> fetchAndDecrypt(final String userId, final String service) {
        val secret = getSecret(service, userId);
        return secret.mapNotNull(s -> s).flatMap(s -> kmsService.decrypt(s.getSecret()));

    }

    public Mono<String> fetchAndDecryptNotExpired(final String userId, final String service) {
        return getSecret(service, userId)
                .filter(Secret::notExpired)
                .flatMap(s -> kmsService.decrypt(s.getSecret()));

    }


}
