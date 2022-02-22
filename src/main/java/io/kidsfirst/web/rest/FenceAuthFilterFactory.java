package io.kidsfirst.web.rest;

import io.kidsfirst.config.AllFences;
import io.kidsfirst.core.service.SecretService;
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
public class FenceAuthFilterFactory extends AbstractGatewayFilterFactory<FenceAuthFilterFactory.Config> {

    private final SecretService secretService;

    public FenceAuthFilterFactory(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) ->
                exchange.getPrincipal()
                        .filter(p -> p instanceof JwtAuthenticationToken)
                        .cast(JwtAuthenticationToken.class)
                        .flatMap(user -> {
                            val userId = user.getTokenAttributes().get("sub").toString();
                            //TODO implement a function that will refresh the token automatically if expired
                            return secretService.fetchAccessToken(config.fence, userId);
                        })
                        .mapNotNull(token -> Optional.of(withBearerAuth(exchange, token)))
                        .defaultIfEmpty(Optional.empty())
                        .flatMap(o -> o.map(chain::filter).orElse(Mono.defer( () -> unauthorized(exchange))));
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
}
