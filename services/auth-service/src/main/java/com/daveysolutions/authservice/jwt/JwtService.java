package com.daveysolutions.authservice.jwt;

import com.daveysolutions.authservice.api.UnauthorizedException;
import com.daveysolutions.authservice.domain.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Service responsible for creating and validating signed JWT access and refresh tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    /**
     * Generates a signed JWT access token for the given user.
     *
     * <p>The token contains:
     * <ul>
     *   <li>{@code sub} — the user's email address</li>
     *   <li>{@code role} — the user's role string</li>
     *   <li>{@code type} — {@code "access"}</li>
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
        return buildJwt(user.getEmail(), user.getRole().name(), TYPE_ACCESS, now, expiry);
    }

    /**
     * Generates a signed JWT refresh token for the given user.
     *
     * <p>The token contains:
     * <ul>
     *   <li>{@code sub} — the user's email address</li>
     *   <li>{@code type} — {@code "refresh"}</li>
     *   <li>{@code iat} — issued-at timestamp</li>
     *   <li>{@code exp} — expiry timestamp (iat + {@link JwtProperties#getRefreshExpirationMs()})</li>
     * </ul>
     *
     * @param user the authenticated user
     * @return compact, URL-safe JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshExpirationMs());
        return buildJwt(user.getEmail(), null, TYPE_REFRESH, now, expiry);
    }

    /**
     * Validates the given refresh token and returns the subject (email) it encodes.
     *
     * <p>Throws {@link UnauthorizedException} if the token is expired, tampered with, or is not
     * a refresh token.
     *
     * @param refreshToken the compact JWT refresh token string
     * @return the subject claim (email address) encoded in the token
     * @throws UnauthorizedException when the token is invalid, expired, or of the wrong type
     */
    public String extractSubjectFromRefreshToken(String refreshToken) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new UnauthorizedException();
            }
            return claims.getSubject();
        } catch (JwtException e) {
            throw new UnauthorizedException();
        }
    }

    /**
     * Constructs and signs a compact JWT string.
     *
     * @param subject  the subject claim (email)
     * @param role     the role claim value, or {@code null} to omit the claim
     * @param type     the token type claim value ({@code "access"} or {@code "refresh"})
     * @param issuedAt issued-at timestamp
     * @param expiry   expiry timestamp
     * @return compact, URL-safe JWT string
     */
    private String buildJwt(String subject, String role, String type, Instant issuedAt, Instant expiry) {
        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry))
                .signWith(signingKey(), Jwts.SIG.HS256);
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
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
