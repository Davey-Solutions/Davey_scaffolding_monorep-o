package com.daveysolutions.authservice.api;

import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRepository;
import com.daveysolutions.authservice.jwt.JwtService;
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
     * Validates credentials and returns a signed JWT access token.
     *
     * <p>Throws {@link UnauthorizedException} (HTTP 401) when the email is not found or the
     * password does not match.
     *
     * @param request login credentials
     * @return {@link LoginResponse} containing the signed access token
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String normalizedEmail = User.normalizeEmail(request.getEmail());

        return userRepository.findByEmail(normalizedEmail)
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .map(user -> new LoginResponse(jwtService.generateToken(user)))
                .orElseThrow(UnauthorizedException::new);
    }
}
