package com.example.TicketFlix.Exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for authentication failures
 */
public class AuthenticationException extends TicketFlixException {
    
    public AuthenticationException(String message) {
        super(message, "AUTHENTICATION_ERROR", HttpStatus.UNAUTHORIZED.value());
    }
    
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }
    
    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid or expired token");
    }
    
    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("Account is disabled. Please contact administrator");
    }
}