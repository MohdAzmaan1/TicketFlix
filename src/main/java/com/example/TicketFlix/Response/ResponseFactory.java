package com.example.TicketFlix.Response;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResponseFactory {

    public static <T> ApiResponse<T> success(T data, String message, HttpServletRequest request) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .requestId(UUID.randomUUID().toString())
                .path(request != null ? request.getRequestURI() : null)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiResponse<Void> ack(String message, HttpServletRequest request) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .requestId(UUID.randomUUID().toString())
                .path(request != null ? request.getRequestURI() : null)
                .timestamp(Instant.now())
                .build();
    }

    public static ApiResponse<Void> failure(String message, HttpServletRequest request) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .requestId(UUID.randomUUID().toString())
                .path(request != null ? request.getRequestURI() : null)
                .timestamp(Instant.now())
                .build();
    }
}

