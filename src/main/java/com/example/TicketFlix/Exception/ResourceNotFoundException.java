package com.example.TicketFlix.Exception;

import org.springframework.http.HttpStatus;

/**
 * Exception when a requested resource is not found
 */
public class ResourceNotFoundException extends TicketFlixException {
    
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier), 
              "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND.value());
    }
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND.value());
    }
}