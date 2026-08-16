package com.daveysolutions.authservice.jwt;

import com.daveysolutions.authservice.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Service responsible for creating signed JWT access tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generates a signed JWT access token for the given user.
     *
     * <p>The token contains:
     * <ul>
     *   <li>{@code sub} — the user's email address</li>
     *   <li>{@code role} — the user's role string</li>
     *   <li>{@code iat} — issued-at timestamp</li>
     *   <li>{@code exp} — expiry timestamp (iat + {@link JwtProperties#getExpirationMs()})</li>
     * </ul>
     *
     * @param user the authenticated user whose claims populate the token
     * @return compact, URL-safe JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getExpirationMs());
        return buildJwt(user.getEmail(), user.getRole().name(), now, expiry);
    }

    /**
     * Constructs and signs a compact JWT string.
     *
     * @param subject the subject claim (email)
     * @param role    the role claim value
     * @param issuedAt  issued-at timestamp
     * @param expiry  expiry timestamp
     * @return compact, URL-safe JWT string
     */
    private String buildJwt(String subject, String role, Instant issuedAt, Instant expiry) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Returns the {@link SecretKey} derived from the configured secret.
     *
     * @return HMAC-SHA256 secret key
     */
    public SecretKey signingKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
