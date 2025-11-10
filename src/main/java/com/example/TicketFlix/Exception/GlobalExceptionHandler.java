package com.example.TicketFlix.Exception;

import com.example.TicketFlix.Response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the TicketFlix application
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketFlixException.class)
    public ResponseEntity<ApiResponse<Void>> handleTicketFlixException(
            TicketFlixException ex, HttpServletRequest request) {
        
        log.error("TicketFlix Exception: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(response, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation Exception: {}", errors);
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.error("Access Denied Exception: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Access denied. You don't have permission to access this resource")
                .errorCode("ACCESS_DENIED")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected Exception: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected error occurred. Please try again later")
                .errorCode("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        log.error("Runtime Exception: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("A runtime error occurred: " + ex.getMessage())
                .errorCode("RUNTIME_ERROR")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
                
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}