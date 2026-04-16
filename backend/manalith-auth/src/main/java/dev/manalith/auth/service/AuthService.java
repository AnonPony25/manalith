package dev.manalith.auth.service;

import dev.manalith.auth.model.OAuthIdentity;
import dev.manalith.auth.model.RefreshToken;
import dev.manalith.auth.model.User;
import dev.manalith.auth.repository.OAuthIdentityRepository;
import dev.manalith.auth.repository.RefreshTokenRepository;
import dev.manalith.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Core authentication service that orchestrates OAuth2 user provisioning,
 * JWT access token issuance, and refresh token lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${manalith.jwt.refresh-token-expiry-days:30}")
    private int refreshTokenExpiryDays;

    // -------------------------------------------------------------------------
    // Public record representing an issued token pair
    // -------------------------------------------------------------------------

    /**
     * A pair of tokens returned on successful authentication or refresh.
     *
     * @param accessToken  short-lived JWT
     * @param refreshToken opaque long-lived token (plain text, never stored)
     * @param userId       the authenticated user's UUID
     * @param displayName  the authenticated user's display name
     */
    public record TokenPair(String accessToken, String refreshToken, UUID userId, String displayName) {}

    // -------------------------------------------------------------------------
    // OAuth2 user provisioning
    // -------------------------------------------------------------------------

    /**
     * Looks up an existing user by OAuth identity or creates a new one.
     * Also persists the {@link OAuthIdentity} link on first login.
     */
    @Transactional
    public User findOrCreateOAuthUser(
            String provider,
            String providerId,
            String email,
            String displayName,
            String avatarUrl) {

        Optional<OAuthIdentity> existingIdentity =
                oAuthIdentityRepository.findByProviderAndProviderId(provider, providerId);

        if (existingIdentity.isPresent()) {
            UUID userId = existingIdentity.get().getUserId();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "OAuthIdentity references non-existent user: " + userId));
        }

        // New OAuth login — create user and identity
        User newUser = User.builder()
                .displayName(displayName != null ? displayName : "Manalith Player")
                .email(email)
                .avatarUrl(avatarUrl)
                .build();
        newUser = userRepository.save(newUser);

        OAuthIdentity identity = OAuthIdentity.builder()
                .userId(newUser.getId())
                .provider(provider)
                .providerId(providerId)
                .build();
        oAuthIdentityRepository.save(identity);

        log.info("Created new user {} via {} OAuth", newUser.getId(), provider);
        return newUser;
    }

    // -------------------------------------------------------------------------
    // Token issuance
    // -------------------------------------------------------------------------

    /**
     * Issues a new {@link TokenPair} for the given user.
     * A new refresh token is generated, hashed, and persisted.
     */
    @Transactional
    public TokenPair issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        // Generate an opaque refresh token (raw UUID string)
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, rawRefreshToken, user.getId(), user.getDisplayName());
    }

    // -------------------------------------------------------------------------
    // Token refresh (rotation)
    // -------------------------------------------------------------------------

    /**
     * Validates the incoming raw refresh token, rotates it, and issues a new pair.
     *
     * @param rawRefreshToken the plain-text refresh token received from the client
     * @return a new {@link TokenPair}
     * @throws RefreshTokenException if the token is invalid, expired, or revoked
     */
    @Transactional
    public TokenPair refreshAccessToken(String rawRefreshToken) {
        String tokenHash = sha256Hex(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new RefreshTokenException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenException("Refresh token has expired");
        }

        // Revoke the old token (rotation)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Refresh token references non-existent user: " + stored.getUserId()));

        return issueTokenPair(user);
    }

    // -------------------------------------------------------------------------
    // Revocation / logout
    // -------------------------------------------------------------------------

    /**
     * Revokes all active refresh tokens for a user (full logout from all devices).
     */
    @Transactional
    public void revokeAllRefreshTokens(UUID userId) {
        refreshTokenRepository.deleteByUserIdAndRevokedFalse(userId);
        log.info("Revoked all refresh tokens for user {}", userId);
    }

    // -------------------------------------------------------------------------
    // Scheduled cleanup
    // -------------------------------------------------------------------------

    /**
     * Runs daily at 03:00 to purge expired refresh tokens from the database.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running scheduled cleanup of expired refresh tokens");
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // -------------------------------------------------------------------------
    // Exception type
    // -------------------------------------------------------------------------

    public static class RefreshTokenException extends RuntimeException {
        public RefreshTokenException(String message) {
            super(message);
        }
    }
}
