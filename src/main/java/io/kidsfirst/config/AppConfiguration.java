package io.kidsfirst.config;

import io.kidsfirst.core.service.JWTService;
import io.kidsfirst.web.filters.AuthenticationFilter;
import io.kidsfirst.web.filters.RequestWrapperFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class AppConfiguration extends WebSecurityConfigurerAdapter implements WebMvcConfigurer {

    private Environment env;
    private JWTService jwtService;

    public AppConfiguration(JWTService jwtService, Environment env){
        this.env = env;
        this.jwtService = jwtService;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers(HttpMethod.OPTIONS, "/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors();
    }

    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterRegistrationBean() {
        FilterRegistrationBean<RequestWrapperFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setOrder(1);
        registrationBean.setFilter(new RequestWrapperFilter());
        registrationBean.addUrlPatterns("/**");

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistrationBean() {
        FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setOrder(2);
        registrationBean.setFilter(new AuthenticationFilter(jwtService));
        registrationBean.addUrlPatterns("/cavatica","/refresh","/token","/key-store");

        return registrationBean;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        String allowedDomains = env.getProperty("application.corsAllowedDomains", "*");
        String[] allowedDomainsArr = allowedDomains.split("\\|");
        for(int i=0; i<allowedDomainsArr.length; i++){
            config.addAllowedOrigin(allowedDomainsArr[i].trim());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
