package com.daveysolutions.authservice.api;

import com.daveysolutions.authservice.config.JwtProperties;
import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRepository;
import com.daveysolutions.authservice.domain.UserRole;
import com.daveysolutions.authservice.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code POST /api/v1/auth/refresh}.
 *
 * <p>Covers: valid refresh token, expired refresh token, and invalid (malformed) refresh token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshControllerTest {

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "correct-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new User(EMAIL, passwordEncoder.encode(PASSWORD), UserRole.OWNER));
    }

    @Test
    void refreshWithValidRefreshTokenReturnsNewAccessToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(EMAIL);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }

    @Test
    void refreshWithExpiredRefreshTokenReturnsUnauthorized() throws Exception {
        String expiredToken = buildExpiredRefreshToken(EMAIL);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(expiredToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithMalformedTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("not.a.valid.jwt"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithAccessTokenInsteadOfRefreshTokenReturnsUnauthorized() throws Exception {
        String accessToken = jwtService.generateAccessToken(EMAIL);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(accessToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithBlankTokenReturnsClientError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(""))))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Builds a JWT refresh token that was already expired when issued (expiry in the past).
     */
    private String buildExpiredRefreshToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("type", "refresh")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(1)))
                .signWith(key)
                .compact();
    }
}
