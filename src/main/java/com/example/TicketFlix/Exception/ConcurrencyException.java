package com.example.TicketFlix.Exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for concurrency-related issues (seat booking conflicts, etc.)
 */
public class ConcurrencyException extends TicketFlixException {
    
    public ConcurrencyException(String message) {
        super(message, "CONCURRENCY_ERROR", HttpStatus.CONFLICT.value());
    }
    
    public ConcurrencyException(String resource, String operation) {
        super(String.format("Concurrency conflict while %s on %s", operation, resource), 
              "CONCURRENCY_ERROR", HttpStatus.CONFLICT.value());
    }
}