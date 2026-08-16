package com.daveysolutions.authservice.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/refresh}.
 *
 * @param refreshToken a valid, non-expired refresh JWT
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
