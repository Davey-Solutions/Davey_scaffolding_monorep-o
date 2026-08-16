package com.daveysolutions.authservice.jwt;

import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link JwtService}.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-32";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    /** Sets up a {@link JwtService} with a fixed test secret before each test. */
    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(EXPIRATION_MS);
        jwtService = new JwtService(props);
    }

    @Test
    void generatedAccessTokenContainsSubjectAndRoleClaims() {
        User user = new User("user@example.com", "hash", UserRole.OWNER);
        String token = jwtService.generateAccessToken(user);

        Claims claims = parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
    }

    @Test
    void generatedAccessTokenExpiresAfterConfiguredDuration() {
        User user = new User("user@example.com", "hash", UserRole.OWNER);
        long beforeMs = System.currentTimeMillis();
        String token = jwtService.generateAccessToken(user);
        long afterMs = System.currentTimeMillis();

        Claims claims = parseClaims(token);
        Date expiry = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        // Expiry should be approximately iat + expirationMs
        long actualDurationMs = expiry.getTime() - issuedAt.getTime();
        assertThat(actualDurationMs).isCloseTo(EXPIRATION_MS, within(1000L));

        // Expiry must be in the future
        assertThat(expiry.getTime()).isGreaterThan(afterMs);
        // Issued-at must be within the test window
        assertThat(issuedAt.getTime()).isBetween(beforeMs - 1000, afterMs + 1000);
    }

    @Test
    void generatedAccessTokenIsVerifiableWithSameKey() {
        User user = new User("user@example.com", "hash", UserRole.OWNER);
        String token = jwtService.generateAccessToken(user);

        // Parsing with the same key must not throw
        Claims claims = parseClaims(token);
        assertThat(claims).isNotNull();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtService.signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
