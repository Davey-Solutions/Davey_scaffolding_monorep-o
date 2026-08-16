package com.daveysolutions.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for JWT token generation and validation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /** Plain-text HMAC-SHA256 secret used to sign tokens (must be at least 32 characters). */
    private String secret;

    /** Lifetime of an access token. Defaults to 15 minutes. */
    private Duration accessTokenExpiry = Duration.ofMinutes(15);

    /** Lifetime of a refresh token. Defaults to 7 days. */
    private Duration refreshTokenExpiry = Duration.ofDays(7);
}
