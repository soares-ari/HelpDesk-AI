package com.helpdeskai.security;

import com.helpdeskai.exception.AuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Provider for JWT token generation and validation.
 * Uses jjwt library (io.jsonwebtoken) for secure token handling.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.issuer:helpdesk-ai}")
    private String issuer;

    @Value("${jwt.audience:helpdesk-ai-users}")
    private String audience;

    /**
     * Generates a JWT token for a user.
     *
     * @param userId User ID to encode in token
     * @param email User email to encode in token
     * @return JWT token string
     */
    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        SecretKey key = getSigningKey();

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        log.debug("JWT token generated for user ID: {}", userId);
        return token;
    }

    /**
     * Extracts user ID from JWT token.
     *
     * @param token JWT token
     * @return User ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extracts email from JWT token.
     *
     * @param token JWT token
     * @return User email
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Validates a JWT token.
     *
     * @param token JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;

        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Parses and validates JWT token, returning claims.
     *
     * @param token JWT token
     * @return Claims object
     * @throws AuthenticationException if token is invalid
     */
    private Claims parseToken(String token) {
        try {
            SecretKey key = getSigningKey();

            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new AuthenticationException("Invalid or expired JWT token", e);
        }
    }

    /**
     * Gets the signing key from the secret.
     *
     * @return SecretKey for HMAC-SHA256
     */
    private SecretKey getSigningKey() {
        // Ensure secret is at least 256 bits (32 bytes) for HS256
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            log.warn("JWT secret is less than 256 bits. Using HMAC-SHA key derivation.");
            return Keys.hmacShaKeyFor(padSecret(keyBytes));
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Pads secret to minimum 256 bits if needed.
     *
     * @param secret Original secret bytes
     * @return Padded secret (32 bytes minimum)
     */
    private byte[] padSecret(byte[] secret) {
        byte[] padded = new byte[32];
        System.arraycopy(secret, 0, padded, 0, Math.min(secret.length, 32));
        return padded;
    }
}
