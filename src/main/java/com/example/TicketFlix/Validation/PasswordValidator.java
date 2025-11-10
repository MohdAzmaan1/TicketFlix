package com.example.TicketFlix.Validation;

import com.example.TicketFlix.Exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Password validation utility
 */
@Component
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    
    // Regex patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    private static final Pattern NO_WHITESPACE_PATTERN = Pattern.compile("^\\S*$");
    
    public void validatePassword(String password) throws ValidationException {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("password", "Password is required");
        }
        
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (password.length() > MAX_LENGTH) {
            errors.add("Password must not exceed " + MAX_LENGTH + " characters");
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one digit");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("Password must contain at least one special character");
        }
        
        if (!NO_WHITESPACE_PATTERN.matcher(password).matches()) {
            errors.add("Password must not contain whitespace characters");
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("password", String.join("; ", errors));
        }
    }
    
    public boolean isPasswordSecure(String password) {
        try {
            validatePassword(password);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
}