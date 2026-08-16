package com.daveysolutions.authservice.api;

/**
 * Response body returned on successful authentication.
 *
 * @param accessToken  signed JWT access token
 * @param refreshToken signed JWT refresh token
 */
public record LoginResponse(String accessToken, String refreshToken) {
}
