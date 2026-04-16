package dev.manalith.auth.service;

import dev.manalith.auth.model.OAuthIdentity;
import dev.manalith.auth.model.RefreshToken;
import dev.manalith.auth.model.User;
import dev.manalith.auth.repository.OAuthIdentityRepository;
import dev.manalith.auth.repository.RefreshTokenRepository;
import dev.manalith.auth.repository.UserRepository;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService} using Mockito.
 * All dependencies are mocked — no Spring context or database required.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OAuthIdentityRepository oAuthIdentityRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Build a real JwtService with a fresh secret
        String secret = Encoders.BASE64.encode(
                Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());
        jwtService = new JwtService(secret, 15);

        authService = new AuthService(
                userRepository,
                oAuthIdentityRepository,
                refreshTokenRepository,
                jwtService
        );

        // Default stubs: save() returns the argument unchanged (with an ID set).
        // Marked lenient because not every test exercises every repository —
        // Mockito strict mode would otherwise raise UnnecessaryStubbingException.
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                // Simulate DB-generated UUID
                return User.builder()
                        .id(UUID.randomUUID())
                        .displayName(u.getDisplayName())
                        .email(u.getEmail())
                        .avatarUrl(u.getAvatarUrl())
                        .build();
            }
            return u;
        });
        lenient().when(oAuthIdentityRepository.save(any(OAuthIdentity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // findOrCreateOAuthUser — new user
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findOrCreateOAuthUser creates a new User and OAuthIdentity when identity does not exist")
    void findOrCreateOAuthUser_newUser_createsUserAndIdentity() {
        when(oAuthIdentityRepository.findByProviderAndProviderId("github", "gh-123"))
                .thenReturn(Optional.empty());

        User result = authService.findOrCreateOAuthUser(
                "github", "gh-123",
                "jace@themaind.sculptor", "Jace Beleren",
                "https://avatars.example.com/jace.png");

        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("Jace Beleren");
        assertThat(result.getEmail()).isEqualTo("jace@themaind.sculptor");

        verify(userRepository).save(any(User.class));

        ArgumentCaptor<OAuthIdentity> identityCaptor = ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(oAuthIdentityRepository).save(identityCaptor.capture());
        OAuthIdentity savedIdentity = identityCaptor.getValue();
        assertThat(savedIdentity.getProvider()).isEqualTo("github");
        assertThat(savedIdentity.getProviderId()).isEqualTo("gh-123");
    }

    // -------------------------------------------------------------------------
    // findOrCreateOAuthUser — existing identity
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findOrCreateOAuthUser returns the existing User when the OAuthIdentity already exists")
    void findOrCreateOAuthUser_existingIdentity_returnsExistingUser() {
        UUID existingUserId = UUID.randomUUID();
        OAuthIdentity existingIdentity = OAuthIdentity.builder()
                .id(UUID.randomUUID())
                .userId(existingUserId)
                .provider("discord")
                .providerId("dc-456")
                .build();
        User existingUser = User.builder()
                .id(existingUserId)
                .displayName("Liliana Vess")
                .email("liliana@undead.realm")
                .build();

        when(oAuthIdentityRepository.findByProviderAndProviderId("discord", "dc-456"))
                .thenReturn(Optional.of(existingIdentity));
        when(userRepository.findById(existingUserId))
                .thenReturn(Optional.of(existingUser));

        User result = authService.findOrCreateOAuthUser(
                "discord", "dc-456",
                "liliana@undead.realm", "Liliana Vess", null);

        assertThat(result.getId()).isEqualTo(existingUserId);
        assertThat(result.getDisplayName()).isEqualTo("Liliana Vess");

        // No new user should have been created
        verify(userRepository, never()).save(any(User.class));
    }

    // -------------------------------------------------------------------------
    // refreshAccessToken — valid token rotation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refreshAccessToken issues a new token pair and revokes the old refresh token")
    void refreshAccessToken_validToken_rotatesToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).displayName("Chandra Nalaar").email("chandra@fire.magic").build();

        // Build a stored (non-expired, non-revoked) refresh token
        // We need to know the hash of our raw token to stub the repository
        String rawToken = "test-raw-refresh-token-" + UUID.randomUUID();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(storedToken));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        AuthService.TokenPair result = authService.refreshAccessToken(rawToken);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.displayName()).isEqualTo("Chandra Nalaar");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        // Old token should have been revoked
        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class)); // revoke + new
    }

    // -------------------------------------------------------------------------
    // refreshAccessToken — expired token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("refreshAccessToken throws RefreshTokenException when the token has expired")
    void refreshAccessToken_expiredToken_throwsException() {
        String rawToken = "expired-token-" + UUID.randomUUID();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // already expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshAccessToken(rawToken))
                .isInstanceOf(AuthService.RefreshTokenException.class)
                .hasMessageContaining("expired");
    }

    // -------------------------------------------------------------------------
    // Helper — must match the logic in AuthService
    // -------------------------------------------------------------------------

    private static String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
