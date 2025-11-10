package com.example.TicketFlix.Exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for business logic violations
 */
public class BusinessException extends TicketFlixException {
    
    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST.value());
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, "BUSINESS_ERROR", HttpStatus.BAD_REQUEST.value(), cause);
    }
}