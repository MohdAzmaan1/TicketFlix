package com.example.TicketFlix.Validation;

import com.example.TicketFlix.Exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();
    }

    @Test
    void testValidatePassword_ValidPassword() {
        // Arrange
        String validPassword = "SecurePassword123!";

        // Act & Assert
        assertDoesNotThrow(() -> {
            passwordValidator.validatePassword(validPassword);
        });
    }

    @Test
    void testValidatePassword_NullPassword() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(null);
        });

        assertTrue(exception.getMessage().contains("Password is required"));
    }

    @Test
    void testValidatePassword_EmptyPassword() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword("");
        });

        assertTrue(exception.getMessage().contains("Password is required"));
    }

    @Test
    void testValidatePassword_TooShort() {
        // Arrange
        String shortPassword = "Short1!";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(shortPassword);
        });

        assertTrue(exception.getMessage().contains("Password must be at least 8 characters long"));
    }

    @Test
    void testValidatePassword_TooLong() {
        // Arrange
        String longPassword = "A".repeat(130) + "1!";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(longPassword);
        });

        assertTrue(exception.getMessage().contains("Password must not exceed 128 characters"));
    }

    @Test
    void testValidatePassword_NoUppercase() {
        // Arrange
        String noUppercasePassword = "securepassword123!";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(noUppercasePassword);
        });

        assertTrue(exception.getMessage().contains("Password must contain at least one uppercase letter"));
    }

    @Test
    void testValidatePassword_NoLowercase() {
        // Arrange
        String noLowercasePassword = "SECUREPASSWORD123!";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(noLowercasePassword);
        });

        assertTrue(exception.getMessage().contains("Password must contain at least one lowercase letter"));
    }

    @Test
    void testValidatePassword_NoDigit() {
        // Arrange
        String noDigitPassword = "SecurePassword!";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(noDigitPassword);
        });

        assertTrue(exception.getMessage().contains("Password must contain at least one digit"));
    }

    @Test
    void testValidatePassword_NoSpecialCharacter() {
        // Arrange
        String noSpecialCharPassword = "SecurePassword123";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(noSpecialCharPassword);
        });

        assertTrue(exception.getMessage().contains("Password must contain at least one special character"));
    }

    @Test
    void testValidatePassword_ContainsWhitespace() {
        // Arrange
        String whitespacePassword = "Secure Password123!";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(whitespacePassword);
        });

        assertTrue(exception.getMessage().contains("Password must not contain whitespace characters"));
    }

    @Test
    void testValidatePassword_MultipleErrors() {
        // Arrange
        String weakPassword = "weak";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            passwordValidator.validatePassword(weakPassword);
        });

        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("Password must be at least 8 characters long"));
        assertTrue(errorMessage.contains("Password must contain at least one uppercase letter"));
        assertTrue(errorMessage.contains("Password must contain at least one digit"));
        assertTrue(errorMessage.contains("Password must contain at least one special character"));
    }

    @Test
    void testIsPasswordSecure_ValidPassword() {
        // Arrange
        String validPassword = "SecurePassword123!";

        // Act
        boolean result = passwordValidator.isPasswordSecure(validPassword);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsPasswordSecure_InvalidPassword() {
        // Arrange
        String invalidPassword = "weak";

        // Act
        boolean result = passwordValidator.isPasswordSecure(invalidPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidatePassword_EdgeCaseSpecialCharacters() {
        // Test various special characters
        String[] validPasswords = {
            "SecurePassword123!",
            "SecurePassword123@",
            "SecurePassword123#",
            "SecurePassword123$",
            "SecurePassword123%",
            "SecurePassword123^",
            "SecurePassword123&",
            "SecurePassword123*",
            "SecurePassword123(",
            "SecurePassword123)",
            "SecurePassword123_",
            "SecurePassword123+",
            "SecurePassword123-",
            "SecurePassword123=",
            "SecurePassword123[",
            "SecurePassword123]",
            "SecurePassword123{",
            "SecurePassword123}",
            "SecurePassword123;",
            "SecurePassword123:",
            "SecurePassword123'",
            "SecurePassword123\"",
            "SecurePassword123\\",
            "SecurePassword123|",
            "SecurePassword123,",
            "SecurePassword123.",
            "SecurePassword123<",
            "SecurePassword123>",
            "SecurePassword123/",
            "SecurePassword123?"
        };

        for (String password : validPasswords) {
            assertDoesNotThrow(() -> {
                passwordValidator.validatePassword(password);
            }, "Password with special character should be valid: " + password);
        }
    }
}