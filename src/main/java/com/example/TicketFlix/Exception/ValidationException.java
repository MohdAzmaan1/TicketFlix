package com.example.TicketFlix.Exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for input validation failures
 */
public class ValidationException extends TicketFlixException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value());
    }
    
    public ValidationException(String field, String reason) {
        super(String.format("Validation failed for field '%s': %s", field, reason), 
              "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value());
    }
}