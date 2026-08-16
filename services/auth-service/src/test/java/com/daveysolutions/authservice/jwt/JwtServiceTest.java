package com.daveysolutions.authservice.jwt;

import com.daveysolutions.authservice.config.JwtProperties;
import com.daveysolutions.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-chars!";

    private JwtService jwtService;

    /** Sets up a {@link JwtService} with a fixed test secret before each test. */
    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        jwtService = new JwtService(props);
    }

    @Test
    void accessTokenContainsSubjectAndTypeAccessClaim() {
        String token = jwtService.generateAccessToken("user@example.com");

        Claims claims = parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("type", String.class)).isEqualTo(JwtService.TYPE_ACCESS);
    }

    @Test
    void refreshTokenContainsSubjectAndTypeRefreshClaim() {
        String token = jwtService.generateRefreshToken("user@example.com");

        Claims claims = parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("type", String.class)).isEqualTo(JwtService.TYPE_REFRESH);
    }

    @Test
    void extractSubjectFromRefreshTokenReturnsCorrectSubject() {
        String token = jwtService.generateRefreshToken("user@example.com");

        assertThat(jwtService.extractSubjectFromRefreshToken(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractSubjectFromRefreshTokenThrowsForAccessToken() {
        String token = jwtService.generateAccessToken("user@example.com");

        assertThatThrownBy(() -> jwtService.extractSubjectFromRefreshToken(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
