package dev.manalith.auth.dto;

/**
 * Request body for {@code POST /api/auth/refresh}.
 *
 * @param refreshToken the opaque refresh token previously issued by the server
 */
public record RefreshTokenRequest(String refreshToken) {}
