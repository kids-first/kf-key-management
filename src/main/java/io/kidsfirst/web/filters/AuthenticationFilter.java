package io.kidsfirst.web.filters;

import io.kidsfirst.core.service.JWTService;
import lombok.val;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationFilter extends OncePerRequestFilter {

    private JWTService jwtService;

    public AuthenticationFilter(JWTService jwtService){
        this.jwtService = jwtService;
    }

    public void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {

        if(HttpMethod.OPTIONS.name() != httpServletRequest.getMethod()) {
            try {
                val auth = httpServletRequest.getHeader("Authorization");

                // Process header, will throw error if authorization fails
                val token = StringUtils.hasText(auth) ? auth.replace("Bearer ", "") : "";
                val authToken = jwtService.parseToken(token, "ego");

                val userId = jwtService.getUserId(authToken);

                if (userId == null) {
                    throw new IllegalAccessException("Authorization token is missing user ID.");
                }

                Authentication authentication = new UsernamePasswordAuthenticationToken(userId, auth);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                throw new ServletException(e.getMessage());
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
