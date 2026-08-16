package com.daveysolutions.authservice.api;

/**
 * Response body returned on successful authentication.
 *
 * @param accessToken  short-lived JWT for authorising API calls
 * @param refreshToken long-lived JWT for obtaining new access tokens
 */
public record LoginResponse(String accessToken, String refreshToken) {
}
