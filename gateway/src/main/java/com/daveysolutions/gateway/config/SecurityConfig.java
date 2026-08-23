package com.daveysolutions.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Reactive Spring Security configuration for the API gateway.
 *
 * <p>All requests to {@code /api/v1/auth/login} and {@code /api/v1/auth/refresh}
 * are permitted without authentication.  Every other request must carry a valid
 * HMAC-SHA256 signed JWT in the Authorization: ******; the
 * gateway validates the token locally using the shared {@code jwt.secret}.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;

    /**
     * Builds the reactive security filter chain.
     *
     * <ul>
     *   <li>{@code /api/v1/auth/login}   — public (no token required)</li>
     *   <li>{@code /api/v1/auth/refresh} — public (no token required)</li>
     *   <li>{@code /actuator/health}     — public (liveness probe)</li>
     *   <li>{@code /actuator/metrics/**} — public (operational metrics)</li>
     *   <li>Everything else              — authenticated via ******
     * </ul>
     *
     * <p>CSRF protection is disabled because the gateway is fully stateless and
     * authenticates via JWT ****** in the Authorization header — there are
     * no session cookies, so CSRF attacks do not apply.
     *
     * @param http the reactive HTTP security builder
     * @return the configured {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // stateless JWT; no cookies, no CSRF risk
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/actuator/metrics", "/actuator/metrics/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
                )
                .build();
    }

    /**
     * Creates a {@link ReactiveJwtDecoder} that verifies HMAC-SHA256 signatures using
     * the shared secret configured via {@code jwt.secret}.
     *
     * @return a configured {@link NimbusReactiveJwtDecoder}
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }
}
