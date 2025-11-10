package com.example.TicketFlix.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Improved JWT service with better security practices
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret:}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 hours
    private Long expiration;

    @Value("${jwt.refresh.expiration:604800000}") // 7 days
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            // Generate a secure random key if none provided
            byte[] keyBytes = new byte[64]; // 512 bits
            new SecureRandom().nextBytes(keyBytes);
            secretKey = Base64.getEncoder().encodeToString(keyBytes);
            log.warn("Generated random JWT secret key. This should be configured properly in production.");
        }
        
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, int userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("type", "ACCESS");
        return createToken(claims, email, expiration);
    }

    public String generateRefreshToken(String email, int userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "REFRESH");
        return createToken(claims, email, refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Integer extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Integer.class);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("type", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new SecurityException("Invalid JWT token", e);
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true; // Consider expired if we can't parse
        }
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "ACCESS".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating access token: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "REFRESH".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating refresh token: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            return (tokenEmail.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Error validating token for email {}: {}", email, e.getMessage());
            return false;
        }
    }
}