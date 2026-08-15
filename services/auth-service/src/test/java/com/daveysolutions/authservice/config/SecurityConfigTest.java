package com.daveysolutions.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecurityConfig}.
 */
class SecurityConfigTest {

    @Test
    void passwordEncoderUsesBcryptAndHashesPasswords() {
        SecurityConfig securityConfig = new SecurityConfig();

        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String rawPassword = "owner-password-123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("incorrect-password", encodedPassword)).isFalse();
    }
}
