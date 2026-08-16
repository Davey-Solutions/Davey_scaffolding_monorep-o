package com.daveysolutions.authservice.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for the login endpoint.
 */
@Getter
@NoArgsConstructor
public class LoginRequest {

    /** User's email address. */
    @NotBlank
    @Email
    private String email;

    /** User's plain-text password. */
    @NotBlank
    private String password;
}
