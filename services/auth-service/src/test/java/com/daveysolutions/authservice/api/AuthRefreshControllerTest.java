package com.daveysolutions.authservice.api;

import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRepository;
import com.daveysolutions.authservice.domain.UserRole;
import com.daveysolutions.authservice.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc integration tests for {@code POST /api/v1/auth/refresh}.
 *
 * <p>Covers valid, expired, and invalid (tampered) refresh tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMAIL = "refresh-test@example.com";
    private static final String PASSWORD = "some-password";

    private User testUser;

    /** Seeds a test user and captures the entity before each test. */
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = userRepository.save(new User(EMAIL, passwordEncoder.encode(PASSWORD), UserRole.OWNER));
    }

    @Test
    void validRefreshTokenReturns200WithNewAccessToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(testUser);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void expiredRefreshTokenReturns401() throws Exception {
        Instant past = Instant.now().minusSeconds(60);
        String expiredToken = Jwts.builder()
                .subject(EMAIL)
                .claim("type", "refresh")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(jwtService.signingKey(), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", expiredToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRefreshTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "not.a.valid.jwt"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenUsedAsRefreshTokenReturns401() throws Exception {
        String accessToken = jwtService.generateToken(testUser);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", accessToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginResponseIncludesRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }
}
