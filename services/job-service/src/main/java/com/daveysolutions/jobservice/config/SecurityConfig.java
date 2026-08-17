package com.daveysolutions.jobservice.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration for the Job Service (resource-server).
 *
 * <p>All job endpoints require a valid JWT bearer token issued by the auth-service.
 * The actuator health endpoint is exposed without authentication for liveness/readiness probes.
 * CSRF is disabled because the service is stateless and authenticates via bearer tokens only.
 * Unauthenticated requests receive HTTP 401 rather than a redirect.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures HTTP security rules, requiring a valid JWT for all job endpoints.
     *
     * @param http the mutable Spring Security HTTP configuration
     * @param jwtDecoder the JWT decoder used to validate bearer tokens
     * @return the built security filter chain
     * @throws Exception when security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
    }

    /**
     * Creates a {@link JwtDecoder} that validates HMAC-SHA256 signed JWTs using the shared
     * signing secret. The secret is sourced from the {@code JWT_SECRET} environment variable
     * via the {@code jwt.secret} property, matching the key used by the auth-service.
     *
     * @param secret the plain UTF-8 signing secret (must be at least 32 characters)
     * @return a configured {@link NimbusJwtDecoder} for HS256 tokens
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
