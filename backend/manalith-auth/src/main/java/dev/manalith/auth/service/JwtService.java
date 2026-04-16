package dev.manalith.auth.service;

import dev.manalith.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Handles creation, validation, and claim extraction for JWT access tokens.
 * Uses jjwt 0.12.x fluent API.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final int accessTokenExpiryMinutes;

    public JwtService(
            @Value("${manalith.jwt.secret}") String secret,
            @Value("${manalith.jwt.access-token-expiry-minutes:15}") int accessTokenExpiryMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    }

    /**
     * Generates a signed JWT access token for the given user.
     *
     * @param user the authenticated user
     * @return compact JWT string
     */
    public String generateAccessToken(User user) {
        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + (long) accessTokenExpiryMinutes * 60 * 1000;

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("name", user.getDisplayName())
                .claim("email", user.getEmail())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expiryMillis))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token signature and expiry.
     *
     * @param token compact JWT string
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts the user ID (subject claim) from a valid token.
     *
     * @param token compact JWT string
     * @return user UUID
     * @throws JwtAuthenticationException if the token is invalid
     */
    public UUID extractUserId(String token) {
        try {
            String subject = extractClaims(token).getSubject();
            return UUID.fromString(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Cannot extract user ID from token", e);
        }
    }

    /**
     * Extracts all claims from a valid token.
     *
     * @param token compact JWT string
     * @return parsed {@link Claims}
     * @throws JwtAuthenticationException if the token is invalid or expired
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Failed to parse JWT claims", e);
        }
    }
}
