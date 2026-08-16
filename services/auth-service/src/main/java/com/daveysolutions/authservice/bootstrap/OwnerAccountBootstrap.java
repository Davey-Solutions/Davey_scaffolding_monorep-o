package com.daveysolutions.authservice.bootstrap;

import com.daveysolutions.authservice.domain.User;
import com.daveysolutions.authservice.domain.UserRepository;
import com.daveysolutions.authservice.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Optional environment-driven bootstrap that seeds the initial owner account.
 */
@Component
@RequiredArgsConstructor
public class OwnerAccountBootstrap implements ApplicationRunner {

    private static final String OWNER_EMAIL_ENV = "AUTH_BOOTSTRAP_OWNER_EMAIL";
    private static final String OWNER_PASSWORD_ENV = "AUTH_BOOTSTRAP_OWNER_PASSWORD";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates an owner user on startup when bootstrap environment variables are present.
     *
     * @param args startup arguments
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String ownerEmail = System.getenv(OWNER_EMAIL_ENV);
        String ownerPassword = System.getenv(OWNER_PASSWORD_ENV);
        String normalizedEmail = resolveBootstrapEmail(ownerEmail, ownerPassword);

        if (!StringUtils.hasText(normalizedEmail)) {
            return;
        }

        User owner = new User(normalizedEmail, passwordEncoder.encode(ownerPassword), UserRole.OWNER);
        userRepository.save(owner);
    }

    /**
     * Resolves the normalized owner email when bootstrap preconditions are met.
     *
     * @param ownerEmail owner email from environment
     * @param ownerPassword owner password from environment
     * @return normalized owner email when bootstrap should run; otherwise {@code null}
     */
    private String resolveBootstrapEmail(String ownerEmail, String ownerPassword) {
        if (!StringUtils.hasText(ownerEmail) || !StringUtils.hasText(ownerPassword)) {
            return null;
        }

        String normalizedEmail = User.normalizeEmail(ownerEmail);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return null;
        }
        return normalizedEmail;
    }
}
