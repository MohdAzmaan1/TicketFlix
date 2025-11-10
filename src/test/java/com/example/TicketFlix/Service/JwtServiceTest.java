package com.example.TicketFlix.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi10aGF0LWlzLWF0LWxlYXN0LTY0LWJ5dGVzLWxvbmctZm9yLWhzNTEyLWFsZ29yaXRobQ==");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);
    }

    @Test
    void testGenerateAccessToken_Success() {
        // Arrange
        String email = "test@example.com";
        int userId = 1;
        String role = "USER";

        // Act
        String token = jwtService.generateAccessToken(email, userId, role);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testGenerateRefreshToken_Success() {
        // Arrange
        String email = "test@example.com";
        int userId = 1;

        // Act
        String token = jwtService.generateRefreshToken(email, userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void testExtractEmail_Success() {
        // Arrange
        String email = "test@example.com";
        String token = jwtService.generateAccessToken(email, 1, "USER");

        // Act
        String extractedEmail = jwtService.extractEmail(token);

        // Assert
        assertEquals(email, extractedEmail);
    }

    @Test
    void testExtractUserId_Success() {
        // Arrange
        int userId = 123;
        String token = jwtService.generateAccessToken("test@example.com", userId, "USER");

        // Act
        Integer extractedUserId = jwtService.extractUserId(token);

        // Assert
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testExtractRole_Success() {
        // Arrange
        String role = "ADMIN";
        String token = jwtService.generateAccessToken("test@example.com", 1, role);

        // Act
        String extractedRole = jwtService.extractRole(token);

        // Assert
        assertEquals(role, extractedRole);
    }

    @Test
    void testExtractTokenType_AccessToken() {
        // Arrange
        String token = jwtService.generateAccessToken("test@example.com", 1, "USER");

        // Act
        String tokenType = jwtService.extractTokenType(token);

        // Assert
        assertEquals("ACCESS", tokenType);
    }

    @Test
    void testExtractTokenType_RefreshToken() {
        // Arrange
        String token = jwtService.generateRefreshToken("test@example.com", 1);

        // Act
        String tokenType = jwtService.extractTokenType(token);

        // Assert
        assertEquals("REFRESH", tokenType);
    }

    @Test
    void testValidateToken_ValidToken() {
        // Arrange
        String token = jwtService.generateAccessToken("test@example.com", 1, "USER");

        // Act
        Boolean isValid = jwtService.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        Boolean isValid = jwtService.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateAccessToken_ValidAccessToken() {
        // Arrange
        String token = jwtService.generateAccessToken("test@example.com", 1, "USER");

        // Act
        Boolean isValid = jwtService.validateAccessToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateAccessToken_RefreshToken() {
        // Arrange
        String refreshToken = jwtService.generateRefreshToken("test@example.com", 1);

        // Act
        Boolean isValid = jwtService.validateAccessToken(refreshToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateRefreshToken_ValidRefreshToken() {
        // Arrange
        String token = jwtService.generateRefreshToken("test@example.com", 1);

        // Act
        Boolean isValid = jwtService.validateRefreshToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateRefreshToken_AccessToken() {
        // Arrange
        String accessToken = jwtService.generateAccessToken("test@example.com", 1, "USER");

        // Act
        Boolean isValid = jwtService.validateRefreshToken(accessToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateTokenWithEmail_ValidToken() {
        // Arrange
        String email = "test@example.com";
        String token = jwtService.generateAccessToken(email, 1, "USER");

        // Act
        Boolean isValid = jwtService.validateToken(token, email);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithEmail_WrongEmail() {
        // Arrange
        String email = "test@example.com";
        String wrongEmail = "wrong@example.com";
        String token = jwtService.generateAccessToken(email, 1, "USER");

        // Act
        Boolean isValid = jwtService.validateToken(token, wrongEmail);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testExtractExpiration_Success() {
        // Arrange
        String token = jwtService.generateAccessToken("test@example.com", 1, "USER");

        // Act
        var expiration = jwtService.extractExpiration(token);

        // Assert
        assertNotNull(expiration);
        assertTrue(expiration.getTime() > System.currentTimeMillis());
    }

    @Test
    void testGenerateTokenWithEmptySecret() {
        // Arrange
        JwtService jwtServiceWithEmptySecret = new JwtService();
        ReflectionTestUtils.setField(jwtServiceWithEmptySecret, "secretKey", "");
        ReflectionTestUtils.setField(jwtServiceWithEmptySecret, "expiration", 3600000L);

        // Act
        String token = jwtServiceWithEmptySecret.generateAccessToken("test@example.com", 1, "USER");

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractFromMalformedToken() {
        // Arrange
        String malformedToken = "malformed.token";

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            jwtService.extractEmail(malformedToken);
        });
    }
}