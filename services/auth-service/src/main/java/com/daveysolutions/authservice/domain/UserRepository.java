package com.daveysolutions.authservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user account by email address.
     *
     * @param email email address to search for
     * @return optional user matching the email
     */
    Optional<User> findByEmail(String email);
}
