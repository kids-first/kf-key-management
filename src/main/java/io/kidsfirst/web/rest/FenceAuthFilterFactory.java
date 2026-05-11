package io.kidsfirst.web.rest;

import io.kidsfirst.config.AllFences;
import io.kidsfirst.core.service.FenceService;
import io.kidsfirst.core.service.SecretService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Slf4j
public class FenceAuthFilterFactory extends AbstractGatewayFilterFactory<FenceAuthFilterFactory.Config> {

    private final SecretService secretService;
    private final FenceService fenceService;

    public FenceAuthFilterFactory(SecretService secretService, FenceService fenceService) {
        this.secretService = secretService;
        this.fenceService = fenceService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) ->
                exchange.getPrincipal()
                        .filter(p -> p instanceof JwtAuthenticationToken)
                        .cast(JwtAuthenticationToken.class)
                        .flatMap(user -> {
                            val userId = user.getTokenAttributes().get("sub").toString();
                            return fetchAccessTokenAndRefreshIfNeeded(userId, config.fence);
                        })
                        .mapNotNull(token -> Optional.of(withBearerAuth(exchange, token)))
                        .defaultIfEmpty(Optional.empty())
                        .flatMap(o -> o.map(chain::filter).orElse(Mono.defer(() -> unauthorized(exchange))));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private ServerWebExchange withBearerAuth(ServerWebExchange exchange, String token) {
        return exchange.mutate().request(r -> r.headers(headers -> headers.setBearerAuth(token)))
                .build();
    }

    public static class Config {

        private final AllFences.Fence fence;

        public Config(AllFences.Fence fence) {
            this.fence = fence;
        }
    }

    /**
     * KNOWN ISSUE: not single-flight. N parallel requests for the same
     * (user, fence) tuple each independently observe an expired access token
     * and trigger their own refresh, fanning out to the fence's token endpoint.
     *
     * <p>Combined with refresh-token rotation (RFC 6749 §10.4 permits, and
     * RFC 9700 — OAuth 2.0 Security Best Current Practice — effectively
     * requires rotation for public clients; Gen3 and DCF do rotate), this
     * can leave the stored refresh token desynchronized from the fence's
     * view, manifesting as silent re-auth requirements for the user.
     *
     * <p>Pinned by {@link io.kidsfirst.keys.FenceConcurrentRefreshFanOutTest}.
     * Fix deferred to a separate PR — the planned shape is a
     * {@code ConcurrentMap<String, Mono<String>>} populated via
     * {@code computeIfAbsent} + {@code cache()} + {@code doOnTerminate(remove)}
     * so concurrent subscribers share one in-flight refresh.
     */
    public Mono<String> fetchAccessTokenAndRefreshIfNeeded(String userId, AllFences.Fence fence) {
        String key = userId + "_" + fence.getName();
        return secretService.fetchAndDecryptNotExpired(userId, fence.keyAccessToken())
                .switchIfEmpty(Mono.defer(() -> {
                            log.info("Refreshing fence " + key);
                            return secretService
                                    .fetchAndDecryptNotExpired(userId, fence.keyRefreshToken())
                                    .flatMap(refresh -> fenceService.refreshTokens(refresh, fence))
                                    .flatMap(tokens -> secretService.persistTokens(fence, userId, tokens)
                                            .then(Mono.just(tokens.getAccessToken().getValue())));
                        })
                );

    }
}
