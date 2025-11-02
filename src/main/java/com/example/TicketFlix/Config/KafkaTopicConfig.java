package com.example.TicketFlix.Config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic ticketBookingTopic() {
        return TopicBuilder.name("ticket-booking-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketCancellationTopic() {
        return TopicBuilder.name("ticket-cancellation-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailNotificationTopic() {
        return TopicBuilder.name("email-notification")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        return TopicBuilder.name("user-registration-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userCreationTopic() {
        return TopicBuilder.name("user-creation-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDeletionTopic() {
        return TopicBuilder.name("user-deletion-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userUpdateTopic() {
        return TopicBuilder.name("user-update-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Movie topics
    @Bean
    public NewTopic movieCreationTopic() {
        return TopicBuilder.name("movie-creation-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic movieUpdateTopic() {
        return TopicBuilder.name("movie-update-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic movieDeletionTopic() {
        return TopicBuilder.name("movie-deletion-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Theater topics
    @Bean
    public NewTopic theaterCreationTopic() {
        return TopicBuilder.name("theater-creation-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic theaterUpdateTopic() {
        return TopicBuilder.name("theater-update-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic theaterDeletionTopic() {
        return TopicBuilder.name("theater-deletion-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Show topics
    @Bean
    public NewTopic showCreationTopic() {
        return TopicBuilder.name("show-creation-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic showUpdateTopic() {
        return TopicBuilder.name("show-update-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic showDeletionTopic() {
        return TopicBuilder.name("show-deletion-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Ticket topics
    @Bean
    public NewTopic ticketBookingRequestTopic() {
        return TopicBuilder.name("ticket-booking-requests")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketCancellationRequestTopic() {
        return TopicBuilder.name("ticket-cancellation-requests")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Payment topics
    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name("payment-requests")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentResultTopic() {
        return TopicBuilder.name("payment-result-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Screen topics
    @Bean
    public NewTopic screenCreationTopic() {
        return TopicBuilder.name("screen-creation-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic screenUpdateTopic() {
        return TopicBuilder.name("screen-update-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic screenDeletionTopic() {
        return TopicBuilder.name("screen-deletion-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}

