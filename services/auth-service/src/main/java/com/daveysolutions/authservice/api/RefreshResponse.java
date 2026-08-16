package com.daveysolutions.authservice.api;

/**
 * Response body returned after a successful token refresh.
 *
 * @param accessToken fresh signed JWT access token
 */
public record RefreshResponse(String accessToken) {
}
