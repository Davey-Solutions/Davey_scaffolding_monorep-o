package com.daveysolutions.authservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * JPA entity representing a user account.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /** Unique identifier for the user account. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique email address used as the username. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** BCrypt hash of the user's password. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Authorization role assigned to this user. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role = UserRole.OWNER;

    /** Timestamp when the account was created. */
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when the account was last updated. */
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Sets creation and update timestamps before first persistence.
     */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Updates the last-updated timestamp before writes.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Sets the user email using normalized lowercase/trimmed format.
     *
     * @param email user email to assign
     */
    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    /**
     * Normalizes an email address for consistent storage and lookup.
     *
     * @param email email value to normalize
     * @return normalized email, or {@code null} when input is null
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
