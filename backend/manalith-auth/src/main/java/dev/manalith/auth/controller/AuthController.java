package dev.manalith.auth.controller;

import dev.manalith.auth.dto.RefreshTokenRequest;
import dev.manalith.auth.model.User;
import dev.manalith.auth.repository.UserRepository;
import dev.manalith.auth.service.AuthService;
import dev.manalith.auth.service.AuthService.RefreshTokenException;
import dev.manalith.auth.service.AuthService.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoints for auth token management.
 *
 * <ul>
 *   <li>{@code POST /api/auth/refresh} — exchange a refresh token for a new token pair</li>
 *   <li>{@code POST /api/auth/logout}  — revoke all refresh tokens (requires JWT auth)</li>
 *   <li>{@code GET  /api/auth/me}      — return current user profile (requires JWT auth)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Inline DTOs
    // -------------------------------------------------------------------------

    /**
     * User profile response DTO.
     *
     * @param id          the user's UUID as a string
     * @param displayName the user's display name
     * @param email       the user's email (may be null)
     * @param avatarUrl   the user's avatar URL (may be null)
     */
    public record UserProfileDTO(String id, String displayName, String email, String avatarUrl) {}

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    /**
     * Refreshes the access token using a valid refresh token.
     * Implements token rotation: the supplied refresh token is revoked and a new pair is issued.
     *
     * @param request body containing the raw refresh token
     * @return 200 with a new {@link TokenPair}, or 401 if the token is invalid/expired/revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            TokenPair tokenPair = authService.refreshAccessToken(request.refreshToken());
            return ResponseEntity.ok(tokenPair);
        } catch (RefreshTokenException e) {
            log.warn("Refresh token rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Logs out the currently authenticated user by revoking all their active refresh tokens.
     *
     * @param userId the UUID injected from the JWT principal by {@link dev.manalith.auth.config.JwtAuthenticationFilter}
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UUID userId) {
        authService.revokeAllRefreshTokens(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param userId the UUID injected from the JWT principal
     * @return 200 with {@link UserProfileDTO}, or 404 if the user no longer exists
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> me(@AuthenticationPrincipal UUID userId) {
        return userRepository.findById(userId)
                .map(AuthController::toProfileDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private static UserProfileDTO toProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId().toString(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl()
        );
    }
}
