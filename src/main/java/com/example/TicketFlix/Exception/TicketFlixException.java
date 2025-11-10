package com.example.TicketFlix.Exception;

/**
 * Base exception class for all TicketFlix application exceptions
 */
public abstract class TicketFlixException extends Exception {
    private final String errorCode;
    private final int httpStatus;
    
    public TicketFlixException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public TicketFlixException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
}