package com.rc1.mantra_svc.security;

import com.rc1.mantra_svc.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for generating and validating JWT tokens.
 * Uses HMAC-SHA256 signing with a secret loaded from application properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AppProperties appProperties;

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userId      MongoDB user ID stored as a custom claim
     * @param email       subject (username) of the token
     * @param displayName display name claim
     * @return signed compact JWT string
     */
    public String generateToken(String userId, String email, String displayName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("displayName", displayName);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + appProperties.getJwt().getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts all claims from a JWT.
     *
     * @param token the JWT string
     * @return parsed {@link Claims}
     * @throws JwtException if the token is invalid or expired
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** @return the email (subject) embedded in the token */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** @return the userId custom claim embedded in the token */
    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    /**
     * Validates the token against the given email and checks expiry.
     *
     * @param token JWT string
     * @param email expected subject
     * @return {@code true} if valid and not expired
     */
    public boolean isTokenValid(String token, String email) {
        try {
            Claims claims = extractAllClaims(token);
            return email.equals(claims.getSubject()) && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
