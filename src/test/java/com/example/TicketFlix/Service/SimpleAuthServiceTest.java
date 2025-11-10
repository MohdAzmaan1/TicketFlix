package com.example.TicketFlix.Service;

import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UserEntryDTO userEntryDTO;
    private User user;

    @BeforeEach
    void setUp() {
        userEntryDTO = new UserEntryDTO();
        userEntryDTO.setName("Test User");
        userEntryDTO.setEmail("test@example.com");
        userEntryDTO.setPassword("SecurePassword123!");
        userEntryDTO.setAge(25);

        user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setAge(25);
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
    }

    @Test
    void testValidateToken_Success() {
        // Arrange
        when(jwtService.validateToken("validToken")).thenReturn(true);

        // Act
        boolean result = authService.validateToken("validToken");

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Arrange
        when(jwtService.validateToken("invalidToken")).thenReturn(false);

        // Act
        boolean result = authService.validateToken("invalidToken");

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetUserFromToken_Success() throws Exception {
        // Arrange
        when(jwtService.extractEmail("validToken")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        User result = authService.getUserFromToken("validToken");

        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void testGetUserFromToken_UserNotFound() {
        // Arrange
        when(jwtService.extractEmail("validToken")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            authService.getUserFromToken("validToken");
        });

        assertTrue(exception.getMessage().contains("Invalid token"));
    }
}