package com.daveysolutions.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT validation at the gateway.
 *
 * <p>Properties are bound from the {@code jwt.*} namespace and must match the
 * secret used by the auth-service when issuing tokens.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Plain UTF-8 signing secret shared with the auth-service (at least 32 characters
     * for a 256-bit HMAC-SHA256 key).  Sourced from the {@code JWT_SECRET} environment variable.
     */
    private String secret;
}
