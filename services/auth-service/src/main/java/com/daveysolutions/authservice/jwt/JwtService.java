package com.daveysolutions.authservice.jwt;

import com.daveysolutions.authservice.api.UnauthorizedException;
import com.daveysolutions.authservice.domain.User;
import io.jsonwebtoken.Claims;
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
     * Generates a signed access token and a signed refresh token for the given user.
     *
     * @param user the authenticated user
     * @return a {@link TokenPair} containing both tokens
     */
    public TokenPair generateTokenPair(User user) {
        return new TokenPair(generateAccessToken(user), generateRefreshToken(user));
    }

    /**
     * Generates a signed JWT access token for the given user.
     *
     * <p>The token contains {@code sub}, {@code role}, {@code type} ({@code "access"}),
     * {@code iat}, and {@code exp} (iat + {@link JwtProperties#getExpirationMs()}).
     *
     * @param user the authenticated user
     * @return compact, URL-safe JWT access token string
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
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
        Claims claims = parseAndVerifyClaims(refreshToken);
        return requireRefreshType(claims);
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

    /**
     * Generates a signed JWT refresh token for the given user.
     *
     * <p>The token contains {@code sub}, {@code type} ({@code "refresh"}), {@code iat},
     * and {@code exp} (iat + {@link JwtProperties#getRefreshExpirationMs()}).
     *
     * @param user the authenticated user
     * @return compact, URL-safe JWT refresh token string
     */
    private String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshExpirationMs());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and cryptographically verifies a compact JWT string.
     *
     * @param token the compact JWT string to verify
     * @return the verified {@link Claims} payload
     * @throws UnauthorizedException when the token is malformed, tampered with, or expired
     */
    private Claims parseAndVerifyClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new UnauthorizedException();
        }
    }

    /**
     * Asserts that the {@code type} claim equals {@code "refresh"} and returns the subject.
     *
     * @param claims the already-verified JWT payload
     * @return the subject claim (email address)
     * @throws UnauthorizedException when the token is not a refresh token
     */
    private String requireRefreshType(Claims claims) {
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new UnauthorizedException();
        }
        return claims.getSubject();
    }

    /**
     * A pair of signed JWT tokens issued together at login.
     *
     * @param accessToken  short-lived access token
     * @param refreshToken long-lived refresh token
     */
    public record TokenPair(String accessToken, String refreshToken) {}
}
