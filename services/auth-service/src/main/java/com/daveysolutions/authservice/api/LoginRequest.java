package com.daveysolutions.authservice.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Request body for the login endpoint.
 *
 * <p>{@code @Value} makes fields immutable. {@code @AllArgsConstructor} must be declared
 * explicitly because the presence of {@code @NoArgsConstructor} suppresses the all-args
 * constructor otherwise implied by {@code @Value}. {@code @NoArgsConstructor(force = true)}
 * provides the default constructor required by Jackson for bean-style deserialization;
 * Spring Boot's {@code ParameterNamesModule} allows Jackson to resolve and use the all-args
 * constructor automatically.
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class LoginRequest {

    /** User's email address. */
    @NotBlank
    @Email
    String email;

    /** User's plain-text password. */
    @NotBlank
    String password;
}
