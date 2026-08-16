package com.daveysolutions.authservice.jwt;

/**
 * A pair of signed JWT tokens issued together at login.
 *
 * @param accessToken  short-lived access token
 * @param refreshToken long-lived refresh token
 */
public record TokenPair(String accessToken, String refreshToken) {}
