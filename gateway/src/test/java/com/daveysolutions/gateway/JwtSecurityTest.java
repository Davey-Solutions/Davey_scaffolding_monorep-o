package com.daveysolutions.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Integration tests verifying JWT enforcement at the gateway layer.
 *
 * <ul>
 *   <li>Requests to protected endpoints without a token return {@code 401}.</li>
 *   <li>Requests with a tampered / invalid token return {@code 401}.</li>
 *   <li>Requests with a valid, correctly-signed token are authenticated; the gateway
 *       forwards them upstream (resulting in {@code 503} in tests because no real
 *       upstream service is running — but crucially <em>not</em> {@code 401}).</li>
 *   <li>Requests to public auth endpoints are always permitted.</li>
 * </ul>
 *
 * <p>The test application.yml overrides all route URIs to {@code localhost:9999}
 * (an unreachable port) and supplies a fixed {@code jwt.secret}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtSecurityTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-chars!";

    @Autowired
    private WebTestClient webTestClient;

    // -------------------------------------------------------------------------
    // Protected endpoints — /api/v1/jobs/**
    // -------------------------------------------------------------------------

    /**
     * A GET request to a protected route without any {@code Authorization} header
     * must be rejected with {@code 401 Unauthorized}.
     */
    @Test
    void protectedRoute_noToken_returns401() {
        webTestClient.get()
                .uri("/api/v1/jobs/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * A GET request to a protected route with an invalid (tampered) token
     * must be rejected with {@code 401 Unauthorized}.
     */
    @Test
    void protectedRoute_invalidToken_returns401() {
        webTestClient.get()
                .uri("/api/v1/jobs/1")
                .header("Authorization", bearerToken(buildTamperedToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * A GET request to a protected route with a valid, correctly-signed JWT
     * must be authenticated; the gateway forwards the request upstream.
     * Because no upstream service is running in tests, the result is {@code 503}
     * (not {@code 401}), demonstrating that the security layer passed the request.
     */
    @Test
    void protectedRoute_validToken_isForwardedUpstream() {
        webTestClient.get()
                .uri("/api/v1/jobs/1")
                .header("Authorization", bearerToken(buildValidToken()))
                .exchange()
                .expectStatus().value(status -> {
                    // Must NOT be 401 — the JWT was accepted; upstream may return any non-401 code.
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401);
                });
    }

    // -------------------------------------------------------------------------
    // Public auth endpoints
    // -------------------------------------------------------------------------

    /**
     * A POST to {@code /api/v1/auth/login} must be permitted without a token.
     * No upstream service is running, so the response is {@code 503} — but
     * crucially <em>not</em> {@code 401}.
     */
    @Test
    void loginEndpoint_noToken_isPermitted() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    /**
     * A POST to {@code /api/v1/auth/refresh} must be permitted without a token.
     * No upstream service is running, so the response is {@code 503} — but
     * crucially <em>not</em> {@code 401}.
     */
    @Test
    void refreshEndpoint_noToken_isPermitted() {
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus().value(status ->
                        org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Prepends the ****** to a token string to form a valid
     * {@code Authorization} header value.
     *
     * @param token the compact JWT string
     * @return {@code "******"}
     */
    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    /**
     * Builds a signed HMAC-SHA256 JWT using the same secret as the test configuration.
     *
     * @return compact, URL-safe JWT string
     */
    private String buildValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("test@example.com")
                .claim("role", "OWNER")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Builds a JWT signed with a <em>different</em> secret key — valid structure but
     * with an invalid signature relative to {@link #TEST_SECRET}.
     *
     * @return compact, URL-safe JWT string that will fail signature verification
     */
    private String buildTamperedToken() {
        String wrongSecret = "wrong-secret-key-that-is-at-least-32-chars!!";
        SecretKey key = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("attacker@example.com")
                .claim("role", "OWNER")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
