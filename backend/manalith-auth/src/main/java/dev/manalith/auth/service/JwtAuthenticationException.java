package dev.manalith.auth.service;

/**
 * Thrown when a JWT cannot be parsed, validated, or is expired.
 */
public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
