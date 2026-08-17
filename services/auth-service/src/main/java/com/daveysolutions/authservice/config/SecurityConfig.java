package com.daveysolutions.authservice.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Security configuration for the Auth Service.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures HTTP security rules.
     *
     * <p>The actuator health endpoint and the login endpoint are intentionally exposed without
     * authentication. All other requests require authentication. CSRF is disabled because the
     * service is stateless and issues JWTs. Unauthenticated requests to protected endpoints
     * receive HTTP 401 rather than a redirect.
     *
     * @param http the mutable Spring Security HTTP configuration
     * @return the built security filter chain
     * @throws Exception when security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * Creates the password encoder used for storing and verifying user passwords.
     *
     * @return BCrypt password encoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
