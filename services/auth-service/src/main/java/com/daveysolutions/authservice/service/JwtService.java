package com.daveysolutions.authservice.service;

import com.daveysolutions.authservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service responsible for generating and validating JWT access and refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    /** Claim key used to store the token type (access vs refresh). */
    private static final String CLAIM_TOKEN_TYPE = "type";

    /** Token type value for access tokens. */
    public static final String TYPE_ACCESS = "access";

    /** Token type value for refresh tokens. */
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    /**
     * Generates a short-lived access token for the given subject (user email).
     *
     * @param subject the user identifier (email) to embed as the JWT subject
     * @return signed JWT access token string
     */
    public String generateAccessToken(String subject) {
        return buildToken(subject, TYPE_ACCESS, jwtProperties.getAccessTokenExpiry().toMillis());
    }

    /**
     * Generates a long-lived refresh token for the given subject (user email).
     *
     * @param subject the user identifier (email) to embed as the JWT subject
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(String subject) {
        return buildToken(subject, TYPE_REFRESH, jwtProperties.getRefreshTokenExpiry().toMillis());
    }

    /**
     * Extracts the subject (user email) from a valid JWT.
     *
     * @param token the JWT string to inspect
     * @return the subject claim value
     * @throws io.jsonwebtoken.JwtException when the token is invalid or expired
     */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the token type claim from a valid JWT.
     *
     * @param token the JWT string to inspect
     * @return the token type value ({@link #TYPE_ACCESS} or {@link #TYPE_REFRESH})
     * @throws io.jsonwebtoken.JwtException when the token is invalid or expired
     */
    public String extractTokenType(String token) {
        return parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    /**
     * Validates and parses a refresh token, returning its subject (user email) on success.
     *
     * @param token the JWT string to validate
     * @return the subject extracted from the token
     * @throws io.jsonwebtoken.JwtException when the token is invalid, expired, or not a refresh token
     * @throws IllegalArgumentException     when the token is not of type refresh
     */
    public String extractSubjectFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }
        return claims.getSubject();
    }

    private String buildToken(String subject, String tokenType, long expiryMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMillis)))
                .signWith(signingKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
