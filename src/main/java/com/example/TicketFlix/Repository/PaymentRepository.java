package com.example.TicketFlix.Repository;

import com.example.TicketFlix.Models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByPaymentId(String paymentId);
}

