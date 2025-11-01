package com.example.TicketFlix.Kafka;

import com.example.TicketFlix.Service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private MailService mailService;

    // Consumer for email notifications
    @KafkaListener(topics = "email-notification", groupId = "email-group")
    public void consumeEmailNotification(String message) {
        log.info("Received email notification: {}", message);
        
        try {
            // Parse the message: format is "email|subject|body"
            String[] parts = message.split("\\|", 3);
            if (parts.length == 3) {
                String email = parts[0];
                String subject = parts[1];
                String body = parts[2];
                
                log.info("Sending email to: {} with subject: {}", email, subject);
                mailService.sendSimpleMail(email, subject, body);
                log.info("Email sent successfully to: {}", email);
            } else {
                log.error("Invalid email notification format: {}", message);
            }
        } catch (Exception e) {
            log.error("Error processing email notification: {}", e.getMessage());
        }
    }

    // Consumer for ticket booking events
    @KafkaListener(topics = "ticket-booking-events", groupId = "ticket-analytics-group")
    public void consumeTicketBookingEvent(String message) {
        log.info("Ticket booking event received: {}", message);
        // Here we can add analytics processing, logging to a separate database, etc.
        // For example: update dashboards, trigger push notifications, etc.
    }

    // Consumer for ticket cancellation events
    @KafkaListener(topics = "ticket-cancellation-events", groupId = "ticket-analytics-group")
    public void consumeTicketCancellationEvent(String message) {
        log.info("Ticket cancellation event received: {}", message);
        // Here we can add analytics processing, revenue tracking, etc.
    }

    // Consumer for user registration events
    @KafkaListener(topics = "user-registration-events", groupId = "user-analytics-group")
    public void consumeUserRegistrationEvent(String message) {
        log.info("User registration event received: {}", message);
        // Here we can add analytics processing, send welcome emails, etc.
    }
}

