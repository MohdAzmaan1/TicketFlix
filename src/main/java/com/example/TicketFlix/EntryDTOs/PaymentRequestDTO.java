package com.example.TicketFlix.EntryDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {
    private int userId;
    private int ticketId; // Will be null for new bookings, set for retries
    private TicketEntryDTO ticketEntryDTO; // Contains booking details
    private double amount;
    private String paymentMethod; // "CARD", "UPI", "WALLET", "NET_BANKING"
    private String cardNumber; // Last 4 digits or masked
    private String idempotencyKey; // Client-generated unique key for retry prevention
}

