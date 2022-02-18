package io.kidsfirst.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class WebConfiguration {

    private final Environment env;

    public WebConfiguration(Environment env){
        this.env = env;
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        String allowedDomains = env.getProperty("application.cors_allowed_domains", "*");

        String[] allowedDomainsArr = allowedDomains.split("\\|");
        for (String s : allowedDomainsArr) {
            config.addAllowedOriginPattern(s.trim());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("cavatic_route", r -> r.path("/cavatica2/*")
                        .filters(f -> f.rewritePath("cavatica2", "v2"))
                                .uri("https://cavatica-api.sbgenomics.com"))
                .build();
    }

}
