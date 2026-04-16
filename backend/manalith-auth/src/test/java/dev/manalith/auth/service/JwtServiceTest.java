package dev.manalith.auth.service;

import dev.manalith.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}.
 * No Spring context needed — the service only depends on config values.
 */
class JwtServiceTest {

    /**
     * A 256-bit base64-encoded secret sufficient for HMAC-SHA256.
     * Generated from a fixed key so tests are deterministic.
     */
    private static final String TEST_SECRET;

    static {
        SecretKey key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        TEST_SECRET = Encoders.BASE64.encode(key.getEncoded());
    }

    private JwtService jwtService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        // 15-minute expiry for normal tests
        jwtService = new JwtService(TEST_SECRET, 15);

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .displayName("Jace Beleren")
                .email("jace@themaind.sculptor")
                .build();
    }

    // -------------------------------------------------------------------------
    // Round-trip: generate then validate
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateAccessToken produces a token that validateToken accepts")
    void generateAndValidateToken_roundTrip() {
        String token = jwtService.generateAccessToken(sampleUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    // -------------------------------------------------------------------------
    // Claim extraction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractUserId returns the correct UUID from a valid token")
    void extractUserId_fromValidToken() {
        String token = jwtService.generateAccessToken(sampleUser);

        UUID extracted = jwtService.extractUserId(token);

        assertThat(extracted).isEqualTo(sampleUser.getId());
    }

    @Test
    @DisplayName("extractClaims includes name and email claims")
    void extractClaims_containsNameAndEmail() {
        String token = jwtService.generateAccessToken(sampleUser);

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.get("name", String.class)).isEqualTo(sampleUser.getDisplayName());
        assertThat(claims.get("email", String.class)).isEqualTo(sampleUser.getEmail());
    }

    // -------------------------------------------------------------------------
    // Expiry validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateToken returns false for a token with 0-second expiry (immediately expired)")
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        // Create a JwtService with 0-minute expiry — token expires immediately
        JwtService shortLivedService = new JwtService(TEST_SECRET, 0);
        String token = shortLivedService.generateAccessToken(sampleUser);

        // Sleep briefly so expiry definitely passes
        Thread.sleep(1100);

        assertThat(shortLivedService.validateToken(token)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Tamper detection
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateToken returns false for a token with a tampered payload")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtService.generateAccessToken(sampleUser);

        // A JWT has three base64url-encoded parts separated by dots.
        // Appending "x" to the payload section invalidates the signature.
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        String tampered = parts[0] + "." + parts[1] + "x" + "." + parts[2];

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }
}
