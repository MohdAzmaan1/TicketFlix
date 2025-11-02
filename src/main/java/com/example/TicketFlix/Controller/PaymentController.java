package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.PaymentRequestDTO;
import com.example.TicketFlix.Response.PaymentResponseDTO;
import com.example.TicketFlix.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDTO> processPayment(@RequestBody PaymentRequestDTO paymentRequestDTO) {
        try {
            PaymentResponseDTO response = paymentService.processPayment(paymentRequestDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Return error response
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .amount(paymentRequestDTO.getAmount())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(@PathVariable String paymentId) {
        try {
            PaymentResponseDTO response = paymentService.getPaymentStatus(paymentId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .status("NOT_FOUND")
                    .message(e.getMessage())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/status-by-key/{idempotencyKey}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByKey(@PathVariable String idempotencyKey) {
        try {
            PaymentResponseDTO response = paymentService.getPaymentByIdempotencyKey(idempotencyKey);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .status("NOT_FOUND")
                    .message(e.getMessage())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
    }
}

