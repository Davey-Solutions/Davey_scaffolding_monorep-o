package com.daveysolutions.authservice.api;

/**
 * Response body returned on successful authentication.
 *
 * @param accessToken signed JWT access token
 */
public record LoginResponse(String accessToken) {
}
