package com.example.TicketFlix.Models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "payments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String paymentId; // UUID for payment

    @Column(unique = true, nullable = false)
    private String idempotencyKey; // Unique key for idempotency

    @Column(nullable = false)
    private String payloadHash; // Hash of the payment request payload

    @ManyToOne
    @JoinColumn
    private User user;

    private int ticketId; // Nullable for pending payments

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String paymentMethod; // CARD, UPI, WALLET, NET_BANKING

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // SUCCESS, FAILED, PENDING, REFUNDED

    private String failureReason; // If payment fails

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    public enum PaymentStatus {
        SUCCESS, FAILED, PENDING, REFUNDED
    }
}

