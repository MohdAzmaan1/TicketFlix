package com.example.TicketFlix.Kafka;

import com.example.TicketFlix.EntryDTOs.*;
import com.example.TicketFlix.EntryDTOs.PaymentRequestDTO;
import com.example.TicketFlix.Models.Ticket;
import com.example.TicketFlix.Models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Publish ticket booking event
    public void publishTicketBookingEvent(Ticket ticket, User user) {
        try {
            String message = String.format(
                "Ticket booked - ID: %s, Movie: %s, User: %s (%s), Seats: %s, Amount: %d",
                ticket.getTicketId(),
                ticket.getMovieName(),
                user.getName(),
                user.getEmail(),
                ticket.getBookedSeat(),
                ticket.getTotalAmount()
            );
            
            kafkaTemplate.send("ticket-booking-events", ticket.getTicketId(), message);
            log.info("Published ticket booking event for ticket ID: {}", ticket.getTicketId());
        } catch (Exception e) {
            log.error("Error publishing ticket booking event: {}", e.getMessage());
        }
    }

    // Publish ticket cancellation event
    public void publishTicketCancellationEvent(Ticket ticket, User user) {
        try {
            String message = String.format(
                "Ticket cancelled - ID: %s, Movie: %s, User: %s (%s), Seats: %s, Refund Amount: %d",
                ticket.getTicketId(),
                ticket.getMovieName(),
                user.getName(),
                user.getEmail(),
                ticket.getBookedSeat(),
                ticket.getTotalAmount()
            );
            
            kafkaTemplate.send("ticket-cancellation-events", ticket.getTicketId(), message);
            log.info("Published ticket cancellation event for ticket ID: {}", ticket.getTicketId());
        } catch (Exception e) {
            log.error("Error publishing ticket cancellation event: {}", e.getMessage());
        }
    }

    // Publish email notification
    public void publishEmailNotification(String email, String subject, String body) {
        try {
            String message = String.format("%s|%s|%s", email, subject, body);
            kafkaTemplate.send("email-notification", email, message);
            log.info("Published email notification for: {}", email);
        } catch (Exception e) {
            log.error("Error publishing email notification: {}", e.getMessage());
        }
    }

    // Publish user creation event with UserEntryDTO (consumer will persist to DB)
    public void publishUserCreationEvent(UserEntryDTO userEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userEntryDTO", userEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            
            // Use email as key for partitioning
            String key = userEntryDTO.getEmail() != null ? userEntryDTO.getEmail() : "unknown";
            kafkaTemplate.send("user-creation-events", key, message);
            log.info("Published user creation event for email: {}", userEntryDTO.getEmail());
        } catch (Exception e) {
            log.error("Error publishing user creation event: {}", e.getMessage());
        }
    }

    public void publishUserRegistrationEvent(User user) {
        try {
            String message = String.format(
                "User registered - Name: %s, Email: %s, Age: %d",
                user.getName(),
                user.getEmail(),
                user.getAge()
            );
            
            kafkaTemplate.send("user-registration-events", user.getEmail(), message);
            log.info("Published user registration event for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error publishing user registration event: {}", e.getMessage());
        }
    }

    // Publish user deletion event with userId only (consumer will fetch user data and delete)
    public void publishUserDeletionEvent(int userId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userId", userId);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            
            kafkaTemplate.send("user-deletion-events", String.valueOf(userId), message);
            log.info("Published user deletion event for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing user deletion event: {}", e.getMessage());
        }
    }

    // Publish user update event with userId and UserEntryDTO (consumer will persist to DB)
    public void publishUserUpdateEvent(int userId, UserEntryDTO userEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userId", userId);
            eventData.put("userEntryDTO", userEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            
            kafkaTemplate.send("user-update-events", String.valueOf(userId), message);
            log.info("Published user update event for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error publishing user update event: {}", e.getMessage());
        }
    }

    // ========== MOVIE EVENTS ==========
    public void publishMovieCreationEvent(MovieEntryDTO movieEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("movieEntryDTO", movieEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            String key = movieEntryDTO.getMovieName() != null ? movieEntryDTO.getMovieName() : "unknown";
            kafkaTemplate.send("movie-creation-events", key, message);
            log.info("Published movie creation event for: {}", movieEntryDTO.getMovieName());
        } catch (Exception e) {
            log.error("Error publishing movie creation event: {}", e.getMessage());
        }
    }

    public void publishMovieUpdateEvent(int movieId, MovieEntryDTO movieEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("movieId", movieId);
            eventData.put("movieEntryDTO", movieEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("movie-update-events", String.valueOf(movieId), message);
            log.info("Published movie update event for movie ID: {}", movieId);
        } catch (Exception e) {
            log.error("Error publishing movie update event: {}", e.getMessage());
        }
    }

    public void publishMovieDeletionEvent(int movieId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("movieId", movieId);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("movie-deletion-events", String.valueOf(movieId), message);
            log.info("Published movie deletion event for movie ID: {}", movieId);
        } catch (Exception e) {
            log.error("Error publishing movie deletion event: {}", e.getMessage());
        }
    }

    // ========== THEATER EVENTS ==========
    public void publishTheaterCreationEvent(TheaterEntryDTO theaterEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("theaterEntryDTO", theaterEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            String key = theaterEntryDTO.getName() != null ? theaterEntryDTO.getName() : "unknown";
            kafkaTemplate.send("theater-creation-events", key, message);
            log.info("Published theater creation event for: {}", theaterEntryDTO.getName());
        } catch (Exception e) {
            log.error("Error publishing theater creation event: {}", e.getMessage());
        }
    }

    public void publishTheaterUpdateEvent(int theaterId, TheaterEntryDTO theaterEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("theaterId", theaterId);
            eventData.put("theaterEntryDTO", theaterEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("theater-update-events", String.valueOf(theaterId), message);
            log.info("Published theater update event for theater ID: {}", theaterId);
        } catch (Exception e) {
            log.error("Error publishing theater update event: {}", e.getMessage());
        }
    }

    public void publishTheaterDeletionEvent(int theaterId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("theaterId", theaterId);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("theater-deletion-events", String.valueOf(theaterId), message);
            log.info("Published theater deletion event for theater ID: {}", theaterId);
        } catch (Exception e) {
            log.error("Error publishing theater deletion event: {}", e.getMessage());
        }
    }

    // ========== SHOW EVENTS ==========
    public void publishShowCreationEvent(ShowEntryDTO showEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("showEntryDTO", showEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            String key = showEntryDTO.getMovieId() + "-" + showEntryDTO.getTheaterId();
            kafkaTemplate.send("show-creation-events", key, message);
            log.info("Published show creation event for movie ID: {}, theater ID: {}", 
                    showEntryDTO.getMovieId(), showEntryDTO.getTheaterId());
        } catch (Exception e) {
            log.error("Error publishing show creation event: {}", e.getMessage());
        }
    }

    public void publishShowUpdateEvent(int showId, ShowEntryDTO showEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("showId", showId);
            eventData.put("showEntryDTO", showEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("show-update-events", String.valueOf(showId), message);
            log.info("Published show update event for show ID: {}", showId);
        } catch (Exception e) {
            log.error("Error publishing show update event: {}", e.getMessage());
        }
    }

    public void publishShowDeletionEvent(int showId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("showId", showId);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("show-deletion-events", String.valueOf(showId), message);
            log.info("Published show deletion event for show ID: {}", showId);
        } catch (Exception e) {
            log.error("Error publishing show deletion event: {}", e.getMessage());
        }
    }

    // ========== TICKET EVENTS (for DB persistence) ==========
    // Note: publishTicketBookingEvent and publishTicketCancellationEvent already exist for analytics
    // These new methods are for actual DB operations
    
    public void publishTicketBookingRequest(TicketEntryDTO ticketEntryDTO, boolean isValidRequest) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("ticketEntryDTO", ticketEntryDTO);
            eventData.put("isValidRequest", isValidRequest);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            String key = ticketEntryDTO.getUserId() + "-" + ticketEntryDTO.getShowId();
            kafkaTemplate.send("ticket-booking-requests", key, message);
            log.info("Published ticket booking request for user ID: {}, show ID: {}", 
                    ticketEntryDTO.getUserId(), ticketEntryDTO.getShowId());
        } catch (Exception e) {
            log.error("Error publishing ticket booking request: {}", e.getMessage());
        }
    }

    public void publishTicketCancellationRequest(int ticketId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("ticketId", ticketId);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("ticket-cancellation-requests", String.valueOf(ticketId), message);
            log.info("Published ticket cancellation request for ticket ID: {}", ticketId);
        } catch (Exception e) {
            log.error("Error publishing ticket cancellation request: {}", e.getMessage());
        }
    }

    // ========== PAYMENT EVENTS ==========
    
    public void publishPaymentRequest(PaymentRequestDTO paymentRequestDTO, String paymentId, String idempotencyKey) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("paymentRequestDTO", paymentRequestDTO);
            eventData.put("paymentId", paymentId);
            eventData.put("idempotencyKey", idempotencyKey);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("payment-requests", idempotencyKey, message);
            log.info("Published payment request for payment ID: {}, idempotency key: {}", paymentId, idempotencyKey);
        } catch (Exception e) {
            log.error("Error publishing payment request: {}", e.getMessage());
        }
    }

    public void publishPaymentResultEvent(com.example.TicketFlix.Models.Payment payment, boolean success) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("paymentId", payment.getPaymentId());
            eventData.put("idempotencyKey", payment.getIdempotencyKey());
            eventData.put("status", payment.getStatus().name());
            eventData.put("amount", payment.getAmount());
            eventData.put("userId", payment.getUser().getId());
            eventData.put("ticketId", payment.getTicketId());
            eventData.put("success", success);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("payment-result-events", payment.getPaymentId(), message);
            log.info("Published payment result event for payment ID: {}, status: {}", 
                    payment.getPaymentId(), payment.getStatus());
        } catch (Exception e) {
            log.error("Error publishing payment result event: {}", e.getMessage());
        }
    }

    // ========== SCREEN EVENTS ==========
    
    public void publishScreenCreationEvent(com.example.TicketFlix.EntryDTOs.ScreenEntryDTO screenEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("screenEntryDTO", screenEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("screen-creation-events", 
                    String.valueOf(screenEntryDTO.getTheaterId()), message);
            log.info("Published screen creation event for theater ID: {}, screen number: {}", 
                    screenEntryDTO.getTheaterId(), screenEntryDTO.getScreenNumber());
        } catch (Exception e) {
            log.error("Error publishing screen creation event: {}", e.getMessage());
        }
    }

    public void publishScreenUpdateEvent(int screenId, com.example.TicketFlix.EntryDTOs.ScreenEntryDTO screenEntryDTO) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("screenId", screenId);
            eventData.put("screenEntryDTO", screenEntryDTO);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("screen-update-events", String.valueOf(screenId), message);
            log.info("Published screen update event for screen ID: {}", screenId);
        } catch (Exception e) {
            log.error("Error publishing screen update event: {}", e.getMessage());
        }
    }

    public void publishScreenDeletionEvent(int screenId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("screenId", screenId);
            eventData.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(eventData);
            kafkaTemplate.send("screen-deletion-events", String.valueOf(screenId), message);
            log.info("Published screen deletion event for screen ID: {}", screenId);
        } catch (Exception e) {
            log.error("Error publishing screen deletion event: {}", e.getMessage());
        }
    }
}

