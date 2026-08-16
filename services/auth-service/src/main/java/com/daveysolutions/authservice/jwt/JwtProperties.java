package com.daveysolutions.authservice.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT token generation.
 *
 * <p>Properties are bound from the {@code jwt.*} namespace.
 * The {@code jwt.secret} value must be a plain UTF-8 string of at least 32 characters
 * (providing a minimum 256-bit HMAC-SHA256 key).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Plain UTF-8 signing secret (must be at least 32 characters for a 256-bit HMAC-SHA256 key).
     * Sourced from the {@code JWT_SECRET} environment variable via Spring property binding.
     */
    private String secret;

    /**
     * Access-token validity duration in milliseconds.
     * Defaults to 3 600 000 ms (1 hour).
     */
    private long expirationMs = 3_600_000L;

    /**
     * Refresh-token validity duration in milliseconds.
     * Defaults to 604 800 000 ms (7 days).
     */
    private long refreshExpirationMs = 604_800_000L;
}
