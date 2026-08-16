package com.daveysolutions.authservice.api;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Request body for the token-refresh endpoint.
 *
 * <p>{@code @Value} makes fields immutable and generates an all-args constructor.
 * {@code @NoArgsConstructor(force = true)} provides the default constructor required by Jackson.
 * {@code @AllArgsConstructor} restores the all-args constructor suppressed by the combination of
 * {@code @Value} and {@code @NoArgsConstructor}.
 */
@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class RefreshRequest {

    /** The JWT refresh token to exchange for a new access token. */
    @NotBlank
    String refreshToken;
}
