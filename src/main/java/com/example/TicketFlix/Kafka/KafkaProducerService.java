package com.example.TicketFlix.Kafka;

import com.example.TicketFlix.Models.Ticket;
import com.example.TicketFlix.Models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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

    // Publish user registration event
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
}

