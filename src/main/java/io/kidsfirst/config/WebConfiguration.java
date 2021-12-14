package io.kidsfirst.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@Configuration
public class WebConfiguration {

    private final Environment env;

    public WebConfiguration(Environment env){
        this.env = env;
    }

    @Bean
    public CorsFilter corsFilter(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        String allowedDomains = env.getProperty("application.cors_allowed_domains", "*");

        String[] allowedDomainsArr = allowedDomains.split("\\|");
        for(int i=0; i<allowedDomainsArr.length; i++){
            config.addAllowedOriginPattern(allowedDomainsArr[i].trim());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
