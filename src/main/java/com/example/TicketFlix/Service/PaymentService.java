package com.example.TicketFlix.Service;

import com.example.TicketFlix.EntryDTOs.PaymentRequestDTO;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.Payment;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.PaymentRepository;
import com.example.TicketFlix.Repository.UserRepository;
import com.example.TicketFlix.Response.PaymentResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_KEY_PREFIX = "payment::idempotency::";
    private static final int IDEMPOTENCY_TTL_HOURS = 24;

    /**
     * Process payment with idempotency and payload hash verification
     * 
     * @param paymentRequestDTO Payment request with idempotency key
     * @return PaymentResponseDTO with payment status
     */
    public PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequestDTO) throws Exception {
        // Validate required fields
        if (paymentRequestDTO.getIdempotencyKey() == null || paymentRequestDTO.getIdempotencyKey().isEmpty()) {
            throw new Exception("Idempotency key is required");
        }

        String idempotencyKey = paymentRequestDTO.getIdempotencyKey();
        String payloadHash = generatePayloadHash(paymentRequestDTO);

        // Check idempotency in Redis first (fast lookup)
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String cachedPaymentData = redisTemplate.opsForValue().get(redisKey);

        if (cachedPaymentData != null) {
            log.info("Idempotency key found in cache: {}", idempotencyKey);
            // Payment was already processed, return cached response
            PaymentResponseDTO cachedResponse = objectMapper.readValue(cachedPaymentData, PaymentResponseDTO.class);
            
            // Double-check payload hash matches
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent() && existingPayment.get().getPayloadHash().equals(payloadHash)) {
                log.info("Returning cached payment response for idempotency key: {}", idempotencyKey);
                return cachedResponse;
            } else {
                log.warn("Idempotency key exists but payload hash mismatch - possible duplicate with different data");
                throw new Exception("Idempotency key already used with different payment data");
            }
        }

        // Check database for idempotency key
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            
            // Verify payload hash matches
            if (!payment.getPayloadHash().equals(payloadHash)) {
                log.error("Idempotency key reused with different payload. Key: {}, Existing Hash: {}, New Hash: {}", 
                        idempotencyKey, payment.getPayloadHash(), payloadHash);
                throw new Exception("Idempotency key already used with different payment data. Payload hash mismatch.");
            }

            log.info("Payment already processed with this idempotency key: {}", idempotencyKey);
            // Return existing payment result
            return buildPaymentResponse(payment);
        }

        // New payment request - process it
        return processNewPayment(paymentRequestDTO, idempotencyKey, payloadHash);
    }

    /**
     * Process a new payment request
     */
    private PaymentResponseDTO processNewPayment(PaymentRequestDTO paymentRequestDTO, 
                                                   String idempotencyKey, 
                                                   String payloadHash) throws Exception {
        // Validate user exists
        Optional<User> userOptional = userRepository.findById(paymentRequestDTO.getUserId());
        if (userOptional.isEmpty()) {
            throw new Exception("User not found with id: " + paymentRequestDTO.getUserId());
        }

        User user = userOptional.get();

        // Create payment record with PENDING status
        String paymentId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .idempotencyKey(idempotencyKey)
                .payloadHash(payloadHash)
                .user(user)
                .amount(paymentRequestDTO.getAmount())
                .paymentMethod(paymentRequestDTO.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        // Store idempotency key in Redis
        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .paymentId(paymentId)
                .idempotencyKey(idempotencyKey)
                .status("PENDING")
                .amount(paymentRequestDTO.getAmount())
                .message("Payment request received")
                .build();

        // Cache the response in Redis
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
            redisTemplate.opsForValue().set(
                    redisKey, 
                    responseJson, 
                    IDEMPOTENCY_TTL_HOURS, 
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("Failed to cache payment response in Redis: {}", e.getMessage());
        }

        // Process payment asynchronously via Kafka
        // This allows retry logic and prevents blocking the API
        kafkaProducerService.publishPaymentRequest(paymentRequestDTO, paymentId, idempotencyKey);
        
        log.info("Payment request published to Kafka. Payment ID: {}, Idempotency Key: {}", paymentId, idempotencyKey);

        return response;
    }

    /**
     * Generate hash of the payment payload for verification
     */
    private String generatePayloadHash(PaymentRequestDTO paymentRequestDTO) {
        try {
            // Create a normalized payload string for hashing
            // Include all fields that should not change between retries
            String payload = String.format(
                    "userId:%d|amount:%.2f|paymentMethod:%s|showId:%d|seats:%s",
                    paymentRequestDTO.getUserId(),
                    paymentRequestDTO.getAmount(),
                    paymentRequestDTO.getPaymentMethod(),
                    paymentRequestDTO.getTicketEntryDTO() != null ? paymentRequestDTO.getTicketEntryDTO().getShowId() : 0,
                    paymentRequestDTO.getTicketEntryDTO() != null ? 
                            String.join(",", paymentRequestDTO.getTicketEntryDTO().getRequestedSeats()) : ""
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating payload hash: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate payload hash", e);
        }
    }

    /**
     * Build payment response from payment entity
     */
    private PaymentResponseDTO buildPaymentResponse(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getPaymentId())
                .idempotencyKey(payment.getIdempotencyKey())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .ticketId(payment.getTicketId())
                .message(payment.getStatus() == Payment.PaymentStatus.SUCCESS ? 
                        "Payment successful" : 
                        payment.getFailureReason() != null ? payment.getFailureReason() : "Payment pending")
                .build();
    }

    /**
     * Called by Kafka consumer to actually process payment
     * This simulates a dummy payment gateway
     */
    @Transactional
    public PaymentResponseDTO processPaymentInDB(String paymentId, PaymentRequestDTO paymentRequestDTO) throws Exception {
        Optional<Payment> paymentOptional = paymentRepository.findByPaymentId(paymentId);
        if (paymentOptional.isEmpty()) {
            throw new Exception("Payment not found with id: " + paymentId);
        }

        Payment payment = paymentOptional.get();

        // If already processed, return existing result
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS || 
            payment.getStatus() == Payment.PaymentStatus.FAILED) {
            log.info("Payment already processed: {}", paymentId);
            return buildPaymentResponse(payment);
        }

        // Simulate dummy payment processing
        // In real implementation, this would call actual payment gateway
        boolean paymentSuccess = simulateDummyPayment(paymentRequestDTO);

        if (paymentSuccess) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
            log.info("Dummy payment successful for payment ID: {}", paymentId);
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds or payment gateway error");
            log.warn("Dummy payment failed for payment ID: {}", paymentId);
        }

        payment = paymentRepository.save(payment);

        // Update cache
        PaymentResponseDTO response = buildPaymentResponse(payment);
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            String redisKey = IDEMPOTENCY_KEY_PREFIX + payment.getIdempotencyKey();
            redisTemplate.opsForValue().set(
                    redisKey, 
                    responseJson, 
                    IDEMPOTENCY_TTL_HOURS, 
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("Failed to update payment cache: {}", e.getMessage());
        }

        // Publish payment result event
        kafkaProducerService.publishPaymentResultEvent(payment, paymentSuccess);

        // If payment successful and ticket booking requested, trigger ticket booking
        if (paymentSuccess && paymentRequestDTO.getTicketEntryDTO() != null) {
            kafkaProducerService.publishTicketBookingRequest(paymentRequestDTO.getTicketEntryDTO(), true);
            log.info("Ticket booking triggered after successful payment for payment ID: {}", paymentId);
        }

        return response;
    }

    /**
     * Simulate dummy payment gateway
     * Returns true for 80% success rate, false for 20% failure rate
     */
    private boolean simulateDummyPayment(PaymentRequestDTO paymentRequestDTO) {
        // Simulate random payment success/failure
        // In production, this would be replaced with actual payment gateway call
        
        // Simple dummy logic: Amount > 10000 fails (high amount check)
        // Otherwise 90% success rate
        if (paymentRequestDTO.getAmount() > 10000) {
            log.info("Dummy payment rejected: Amount too high (>10000)");
            return false;
        }

        // 90% success rate for normal amounts
        double random = Math.random();
        boolean success = random < 0.90;

        log.info("Dummy payment simulation result: {} (random: {})", success ? "SUCCESS" : "FAILED", random);
        return success;
    }

    /**
     * Get payment status by payment ID
     */
    public PaymentResponseDTO getPaymentStatus(String paymentId) throws Exception {
        Optional<Payment> paymentOptional = paymentRepository.findByPaymentId(paymentId);
        if (paymentOptional.isEmpty()) {
            throw new Exception("Payment not found with id: " + paymentId);
        }

        return buildPaymentResponse(paymentOptional.get());
    }

    /**
     * Get payment by idempotency key
     */
    public PaymentResponseDTO getPaymentByIdempotencyKey(String idempotencyKey) throws Exception {
        Optional<Payment> paymentOptional = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (paymentOptional.isEmpty()) {
            throw new Exception("Payment not found with idempotency key: " + idempotencyKey);
        }

        return buildPaymentResponse(paymentOptional.get());
    }
}

