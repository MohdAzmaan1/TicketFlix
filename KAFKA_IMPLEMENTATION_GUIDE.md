# Kafka Implementation Guide for TicketFlix

## Overview
This document describes the Kafka integration in the TicketFlix (Book My Show) project. Kafka is used to decouple services, enable asynchronous processing, and implement event-driven architecture.

## Kafka Use Cases Implemented

### 1. **Asynchronous Email Notifications** 
**Topic:** `email-notification`

**Problem Solved:**
- Previously, emails were sent synchronously during ticket booking/cancellation, blocking the API response
- Slow email delivery could cause poor user experience

**Solution:**
- Tickets book/cancel operations publish email notifications to Kafka
- A separate consumer sends emails asynchronously
- API responds immediately while emails are processed in the background

**Producer:** `TicketService`
**Consumer:** `KafkaConsumerService.consumeEmailNotification()`

### 2. **Ticket Booking Events**
**Topic:** `ticket-booking-events`

**Use Cases:**
- Analytics and reporting per booking
- Audit logging
- Real-time dashboards
- Trigger push notifications
- Update business intelligence metrics

**Data Published:**
- Ticket ID, Movie name, User details
- Seats booked, Amount charged
- Timestamp

**Consumer:** `KafkaConsumerService.consumeTicketBookingEvent()`

### 3. **Ticket Cancellation Events**
**Topic:** `ticket-cancellation-events`

**Use Cases:**
- Revenue tracking and refunds
- Analytics for cancellation patterns
- Notify theater owners about cancellations
- Trigger inventory restocking
- Update reports

**Data Published:**
- Ticket ID, Movie name, Cancelled seats
- Refund amount
- User information

**Consumer:** `KafkaConsumerService.consumeTicketCancellationEvent()`

### 4. **User Registration Events**
**Topic:** `user-registration-events`

**Use Cases:**
- Send welcome emails
- Track user growth metrics
- Analytics and demographics
- Trigger marketing campaigns
- User onboarding workflows

**Data Published:**
- User name, Email, Age
- Registration timestamp

**Consumer:** `KafkaConsumerService.consumeUserRegistrationEvent()`

## Architecture

```
┌─────────────┐
│  Controller │
└──────┬──────┘
       │
       ▼
┌─────────────┐      ┌──────────────────┐
│   Service   │─────▶│ Kafka Producer   │
└─────────────┘      └────────┬─────────┘
                              │
                              ▼
                         ┌─────────┐
                         │  Kafka  │
                         └────┬────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Kafka Consumer   │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │ Email Service    │
                    │ Analytics        │
                    │ Notifications    │
                    └──────────────────┘
```

## Implementation Details

### Files Created

1. **KafkaTopicConfig.java** - Defines Kafka topics
2. **KafkaProducerService.java** - Publishes messages to Kafka topics
3. **KafkaConsumerService.java** - Consumes messages from Kafka topics
4. **EmailRequest.java** - Model for email requests

### Files Modified

1. **TicketService.java** - Now publishes events to Kafka instead of sending emails directly
2. **UserService.java** - Publishes user registration events
3. **MailService.java** - Added `sendSimpleMail()` method for Kafka consumer
4. **application.yaml** - Added Kafka configuration
5. **pom.xml** - Added Spring Kafka dependency

## Kafka Topics Configuration

All topics are configured with:
- **Partitions:** 3 (for parallel processing)
- **Replicas:** 1 (for development - increase for production)

Topics:
- `ticket-booking-events`
- `ticket-cancellation-events`
- `email-notification`
- `user-registration-events`

## Configuration

### application.yaml
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all  # Wait for all replicas to confirm
    consumer:
      group-id: ticketflix-group
      auto-offset-reset: earliest
```

## How to Run

### Prerequisites
1. Install Kafka: https://kafka.apache.org/quickstart
2. Start Zookeeper: `bin/zookeeper-server-start.sh config/zookeeper.properties`
3. Start Kafka: `bin/kafka-server-start.sh config/server.properties`

### Running the Application
1. Ensure Kafka is running on `localhost:9092`
2. Start the Spring Boot application
3. Topics will be created automatically on startup
4. Watch logs for Kafka events

## Testing

### Test Ticket Booking
```bash
curl -X POST http://localhost:8080/tickets/book \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "showId": 1,
    "requestedSeats": ["1A", "1B"]
  }'
```

### Test User Registration
```bash
curl -X POST http://localhost:8080/user/add \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "age": 25
  }'
```

## Benefits

1. **Scalability:** Handle high traffic without blocking API responses
2. **Reliability:** Messages are persisted; retry on failure
3. **Decoupling:** Services communicate via events
4. **Observability:** Log and monitor events
5. **Analytics:** Process events for insights
6. **Performance:** Async processing improves response times

## Future Enhancements

1. **Payment Integration:** Publish payment events
2. **Theater Notifications:** Notify theaters about bookings
3. **Recommendation Engine:** Process booking events for recommendations
4. **Inventory Management:** Real-time seat availability updates
5. **Audit Logging:** Store all events in a separate database
6. **Dead Letter Queue:** Handle failed messages
7. **Schema Registry:** Use Avro/JSON schema for message validation

## Monitoring

Monitor Kafka topics using:
```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# View messages
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic ticket-booking-events --from-beginning

# Check consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

## Troubleshooting

### Common Issues

1. **Kafka not running:** Ensure Kafka is started before the application
2. **Connection refused:** Check `bootstrap-servers` in application.yaml
3. **Topic not created:** Check KafkaTopicConfig logs
4. **Consumer not receiving:** Verify consumer group-id matches

## Production Recommendations

1. Increase replicas to 3 for high availability
2. Configure retention policies
3. Set up monitoring and alerts
4. Use schema registry
5. Implement proper error handling and retries
6. Add Dead Letter Queue (DLQ) for failed messages
7. Use SSL/SASL for security
8. Configure replication factor for brokers

