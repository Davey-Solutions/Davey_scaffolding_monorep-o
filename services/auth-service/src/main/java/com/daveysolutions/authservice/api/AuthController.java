package com.daveysolutions.authservice.api;

import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRepository;
import com.daveysolutions.authservice.jwt.JwtService;
import com.daveysolutions.authservice.jwt.JwtService.TokenPair;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Validates credentials and returns signed JWT access and refresh tokens.
     *
     * <p>Throws {@link UnauthorizedException} (HTTP 401) when the email is not found or the
     * password does not match.
     *
     * @param request login credentials
     * @return {@link LoginResponse} containing the signed access and refresh tokens
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String normalizedEmail = User.normalizeEmail(request.getEmail());

        return userRepository.findByEmail(normalizedEmail)
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .map(user -> {
                        TokenPair pair = jwtService.generateTokenPair(user);
                        return new LoginResponse(pair.accessToken(), pair.refreshToken());
                })
                .orElseThrow(UnauthorizedException::new);
    }

    /**
     * Exchanges a valid refresh token for a new JWT access token.
     *
     * <p>Throws {@link UnauthorizedException} (HTTP 401) when the refresh token is expired,
     * invalid, or the user can no longer be found.
     *
     * @param request body containing the refresh token
     * @return {@link RefreshResponse} containing a fresh signed access token
     */
    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        String email = jwtService.extractSubjectFromRefreshToken(request.getRefreshToken());

        return userRepository.findByEmail(email)
                .map(user -> new RefreshResponse(jwtService.generateAccessToken(user)))
                .orElseThrow(UnauthorizedException::new);
    }
}
