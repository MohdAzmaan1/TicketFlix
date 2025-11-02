package com.example.TicketFlix.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDTO {
    private String paymentId;
    private String idempotencyKey;
    private String status; // "SUCCESS", "FAILED", "PENDING", "REFUNDED"
    private double amount;
    private String message;
    private int ticketId; // If booking is successful
}

