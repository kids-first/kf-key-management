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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class FenceAuthFilterFactory extends AbstractGatewayFilterFactory<FenceAuthFilterFactory.Config> {

    private final SecretService secretService;
    private final FenceService fenceService;
    private final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<>();

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
