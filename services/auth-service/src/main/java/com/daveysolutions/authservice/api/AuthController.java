package com.daveysolutions.authservice.api;

import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRepository;
import com.daveysolutions.authservice.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for authentication operations.
 *
 * <p>Exposes endpoints under {@code /api/v1/auth}.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user with their email and password and returns JWT tokens.
     *
     * <p>Returns {@code 200 OK} with an access token and refresh token on success.
     * Returns {@code 401 Unauthorized} when credentials are invalid.
     *
     * @param request the login credentials
     * @return token pair on successful authentication
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String normalizedEmail = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return new LoginResponse(
                jwtService.generateAccessToken(normalizedEmail),
                jwtService.generateRefreshToken(normalizedEmail));
    }

    /**
     * Exchanges a valid refresh token for a new access token.
     *
     * <p>Returns {@code 200 OK} with a fresh access token on success.
     * Returns {@code 401 Unauthorized} when the refresh token is expired or invalid.
     *
     * @param request the refresh token
     * @return a new access token
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            String subject = jwtService.extractSubjectFromRefreshToken(request.refreshToken());
            return new LoginResponse(
                    jwtService.generateAccessToken(subject),
                    jwtService.generateRefreshToken(subject));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
    }
}
