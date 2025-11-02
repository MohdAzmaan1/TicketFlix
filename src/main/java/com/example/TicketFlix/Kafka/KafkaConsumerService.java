package com.example.TicketFlix.Kafka;

import com.example.TicketFlix.Convertors.UserConvertor;
import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.EntryDTOs.TicketEntryDTO;
import com.example.TicketFlix.Repository.MovieRepository;
import com.example.TicketFlix.Repository.ShowRepository;
import com.example.TicketFlix.Repository.TheaterRepository;
import com.example.TicketFlix.Repository.UserRepository;
import com.example.TicketFlix.Service.MailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private MailService mailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private com.example.TicketFlix.Service.TheaterService theaterService;

    @Autowired
    private com.example.TicketFlix.Service.ShowService showService;

    @Autowired
    private com.example.TicketFlix.Service.TicketService ticketService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

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

    // Consumer for user creation events - Performs actual DB persistence
    @KafkaListener(topics = "user-creation-events", groupId = "user-analytics-group")
    public void consumeUserCreationEvent(String message) {
        log.info("User creation event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            // Convert UserEntryDTO from Map to object
            @SuppressWarnings("unchecked")
            Map<String, Object> userEntryMap = (Map<String, Object>) eventData.get("userEntryDTO");
            UserEntryDTO userEntryDTO = objectMapper.convertValue(userEntryMap, UserEntryDTO.class);

            // Check if user already exists (idempotency check)
            if (userEntryDTO.getEmail() != null) {
                Optional<User> existingUser = userRepository.findByEmail(userEntryDTO.getEmail());
                if (existingUser.isPresent()) {
                    log.warn("User with email {} already exists in DB, skipping creation", userEntryDTO.getEmail());
                    return;
                }
            }

            // Encrypt password before saving (if password is provided)
            if (userEntryDTO.getPassword() != null && !userEntryDTO.getPassword().isEmpty()) {
                userEntryDTO.setPassword(passwordEncoder.encode(userEntryDTO.getPassword()));
            } else {
                // If no password provided, generate a temporary one (shouldn't happen in normal flow)
                log.warn("No password provided for user creation, generating temporary password");
                userEntryDTO.setPassword(passwordEncoder.encode("TEMP_PASSWORD_" + System.currentTimeMillis()));
            }

            User user = UserConvertor.convertDtoToEntity(userEntryDTO);
            User savedUser = userRepository.save(user);
            log.info("User created successfully in DB - ID: {}, Name: {}, Email: {}", 
                    savedUser.getId(), savedUser.getName(), savedUser.getEmail());

            // Publish registration event for analytics/welcome emails (separate topic)
            kafkaProducerService.publishUserRegistrationEvent(savedUser);

            // Additional processing can be added here:
            // - Send welcome emails
            // - Analytics and demographics tracking
            // - Trigger marketing campaigns
            // - User onboarding workflows
            
        } catch (Exception e) {
            log.error("Error processing user creation event: {}", e.getMessage(), e);
            // In production, consider sending to Dead Letter Queue (DLQ) for retry
        }
    }

    // Consumer for user registration events (analytics/notifications)
    @KafkaListener(topics = "user-registration-events", groupId = "user-analytics-group")
    public void consumeUserRegistrationEvent(String message) {
        log.info("User registration event received: {}", message);
        // Here we can add analytics processing, send welcome emails, etc.
    }

    // Consumer for user deletion events - Performs actual DB deletion
    @KafkaListener(topics = "user-deletion-events", groupId = "user-analytics-group")
    public void consumeUserDeletionEvent(String message) {
        log.info("User deletion event received: {}", message);
        
        try {
            // Parse JSON message
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int userId = (Integer) eventData.get("userId");
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("User with ID {} not found in DB during deletion", userId);
                return;
            }
            
            User user = userOptional.get();
            log.info("Deleting user - ID: {}, Name: {}, Email: {}", userId, user.getName(), user.getEmail());

            userRepository.deleteById(userId);
            log.info("User deleted successfully from DB - ID: {}", userId);
            
            // Additional processing can be added here:
            // - Analytics for user churn tracking
            // - Cleanup related data (tickets, preferences, etc.)
            // - Audit logging to separate database
            // - Notify other services about user deletion
            // - Trigger data retention policies
            
        } catch (Exception e) {
            log.error("Error processing user deletion event: {}", e.getMessage(), e);
            // In production, consider sending to Dead Letter Queue (DLQ) for retry
        }
    }

    @KafkaListener(topics = "user-update-events", groupId = "user-analytics-group")
    public void consumeUserUpdateEvent(String message) {
        log.info("User update event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int userId = (Integer) eventData.get("userId");

            // Convert UserEntryDTO from Map to object
            @SuppressWarnings("unchecked")
            Map<String, Object> userEntryMap = (Map<String, Object>) eventData.get("userEntryDTO");
            UserEntryDTO userEntryDTO = objectMapper.convertValue(userEntryMap, UserEntryDTO.class);

            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("User with ID {} not found in DB during update", userId);
                return;
            }
            
            User user = userOptional.get();
            StringBuilder changes = new StringBuilder();

            if (userEntryDTO.getName() != null && !userEntryDTO.getName().isEmpty()) {
                if (!userEntryDTO.getName().equals(user.getName())) {
                    changes.append("Name: ").append(user.getName()).append(" -> ").append(userEntryDTO.getName()).append("; ");
                }
                user.setName(userEntryDTO.getName());
            }
            if (userEntryDTO.getAge() > 0) {
                if (userEntryDTO.getAge() != user.getAge()) {
                    changes.append("Age: ").append(user.getAge()).append(" -> ").append(userEntryDTO.getAge()).append("; ");
                }
                user.setAge(userEntryDTO.getAge());
            }
            if (userEntryDTO.getEmail() != null && !userEntryDTO.getEmail().isEmpty()) {
                if (!userEntryDTO.getEmail().equals(user.getEmail())) {
                    changes.append("Email: ").append(user.getEmail()).append(" -> ").append(userEntryDTO.getEmail()).append("; ");
                }
                user.setEmail(userEntryDTO.getEmail());
            }
            if (userEntryDTO.getMobileNumber() != null && !userEntryDTO.getMobileNumber().isEmpty()) {
                if (!userEntryDTO.getMobileNumber().equals(user.getMobileNumber())) {
                    changes.append("Mobile: ").append(user.getMobileNumber()).append(" -> ").append(userEntryDTO.getMobileNumber()).append("; ");
                }
                user.setMobileNumber(userEntryDTO.getMobileNumber());
            }
            if (userEntryDTO.getAddress() != null && !userEntryDTO.getAddress().isEmpty()) {
                if (!userEntryDTO.getAddress().equals(user.getAddress())) {
                    changes.append("Address: ").append(user.getAddress()).append(" -> ").append(userEntryDTO.getAddress()).append("; ");
                }
                user.setAddress(userEntryDTO.getAddress());
            }
            
            // Handle password update (if provided)
            if (userEntryDTO.getPassword() != null && !userEntryDTO.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userEntryDTO.getPassword()));
                changes.append("Password updated; ");
            }
            
            // Handle role update (if provided) - only for admin users
            if (userEntryDTO.getRole() != null) {
                user.setRole(userEntryDTO.getRole());
                changes.append("Role: ").append(user.getRole()).append("; ");
            }
            
            userRepository.save(user);
            
            String changeLog = !changes.isEmpty() ? changes.toString() : "No changes detected";
            log.info("User updated successfully in DB - ID: {}, Changes: {}", userId, changeLog);
            
            // Additional processing can be added here:
            // - Audit logging (track what changed)
            // - Sync data to other services (recommendation engine, marketing platform)
            // - Analytics on profile changes
            // - Trigger email notifications for sensitive changes (email, phone)
            // - Update search indexes if using search functionality
            
        } catch (Exception e) {
            log.error("Error processing user update event: {}", e.getMessage(), e);
            // In production, consider sending to Dead Letter Queue (DLQ) for retry
        }
    }

    // ========== MOVIE CONSUMERS ==========
    @KafkaListener(topics = "movie-creation-events", groupId = "movie-analytics-group")
    public void consumeMovieCreationEvent(String message) {
        log.info("Movie creation event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            @SuppressWarnings("unchecked")
            Map<String, Object> movieEntryMap = (Map<String, Object>) eventData.get("movieEntryDTO");
            com.example.TicketFlix.EntryDTOs.MovieEntryDTO movieEntryDTO = objectMapper.convertValue(movieEntryMap, com.example.TicketFlix.EntryDTOs.MovieEntryDTO.class);

            // Check if movie already exists (idempotency)
            Optional<com.example.TicketFlix.Models.Movie> existingMovie = movieRepository.findByMovieName(movieEntryDTO.getMovieName());
            if (existingMovie.isPresent()) {
                log.warn("Movie with name {} already exists in DB, skipping creation", movieEntryDTO.getMovieName());
                return;
            }

            // Convert DTO to Entity and save
            com.example.TicketFlix.Models.Movie movie = com.example.TicketFlix.Convertors.MovieConvertor.convertDtoToEntity(movieEntryDTO);
            com.example.TicketFlix.Models.Movie savedMovie = movieRepository.save(movie);
            log.info("Movie created successfully in DB - ID: {}, Name: {}", savedMovie.getId(), savedMovie.getMovieName());

            // Invalidate cache
            String cacheKey = "movie::" + savedMovie.getId();
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing movie creation event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "movie-update-events", groupId = "movie-analytics-group")
    public void consumeMovieUpdateEvent(String message) {
        log.info("Movie update event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int movieId = (Integer) eventData.get("movieId");

            @SuppressWarnings("unchecked")
            Map<String, Object> movieEntryMap = (Map<String, Object>) eventData.get("movieEntryDTO");
            com.example.TicketFlix.EntryDTOs.MovieEntryDTO movieEntryDTO = objectMapper.convertValue(movieEntryMap, com.example.TicketFlix.EntryDTOs.MovieEntryDTO.class);

            Optional<com.example.TicketFlix.Models.Movie> movieOptional = movieRepository.findById(movieId);
            if (movieOptional.isEmpty()) {
                log.warn("Movie with ID {} not found in DB during update", movieId);
                return;
            }

            com.example.TicketFlix.Models.Movie movie = movieOptional.get();
            
            // Update fields
            if (movieEntryDTO.getMovieName() != null && !movieEntryDTO.getMovieName().isEmpty()) {
                movie.setMovieName(movieEntryDTO.getMovieName());
            }
            if (movieEntryDTO.getRating() > 0) {
                movie.setRating(movieEntryDTO.getRating());
            }
            if (movieEntryDTO.getDuration() > 0) {
                movie.setDuration(movieEntryDTO.getDuration());
            }
            if (movieEntryDTO.getGenre() != null) {
                movie.setGenre(movieEntryDTO.getGenre());
            }
            if (movieEntryDTO.getLanguage() != null) {
                movie.setLanguage(movieEntryDTO.getLanguage());
            }

            movieRepository.save(movie);
            log.info("Movie updated successfully in DB - ID: {}", movieId);

            // Invalidate cache
            String cacheKey = "movie::" + movieId;
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing movie update event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "movie-deletion-events", groupId = "movie-analytics-group")
    public void consumeMovieDeletionEvent(String message) {
        log.info("Movie deletion event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int movieId = (Integer) eventData.get("movieId");

            Optional<com.example.TicketFlix.Models.Movie> movieOptional = movieRepository.findById(movieId);
            if (movieOptional.isEmpty()) {
                log.warn("Movie with ID {} not found in DB during deletion", movieId);
                return;
            }

            movieRepository.deleteById(movieId);
            log.info("Movie deleted successfully from DB - ID: {}", movieId);

            // Invalidate cache
            String cacheKey = "movie::" + movieId;
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing movie deletion event: {}", e.getMessage(), e);
        }
    }

    // ========== THEATER CONSUMERS ==========
    @KafkaListener(topics = "theater-creation-events", groupId = "theater-analytics-group")
    public void consumeTheaterCreationEvent(String message) {
        log.info("Theater creation event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            @SuppressWarnings("unchecked")
            Map<String, Object> theaterEntryMap = (Map<String, Object>) eventData.get("theaterEntryDTO");
            com.example.TicketFlix.EntryDTOs.TheaterEntryDTO theaterEntryDTO = objectMapper.convertValue(theaterEntryMap, com.example.TicketFlix.EntryDTOs.TheaterEntryDTO.class);

            // Create theater in DB (this includes creating theater seats)
            theaterService.createTheaterInDB(theaterEntryDTO);
            log.info("Theater created successfully in DB - Name: {}", theaterEntryDTO.getName());

            // Invalidate cache if needed
            String cacheKey = "theater::*";
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing theater creation event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "theater-update-events", groupId = "theater-analytics-group")
    public void consumeTheaterUpdateEvent(String message) {
        log.info("Theater update event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int theaterId = (Integer) eventData.get("theaterId");

            @SuppressWarnings("unchecked")
            Map<String, Object> theaterEntryMap = (Map<String, Object>) eventData.get("theaterEntryDTO");
            com.example.TicketFlix.EntryDTOs.TheaterEntryDTO theaterEntryDTO = objectMapper.convertValue(theaterEntryMap, com.example.TicketFlix.EntryDTOs.TheaterEntryDTO.class);

            Optional<com.example.TicketFlix.Models.Theater> theaterOptional = theaterRepository.findById(theaterId);
            if (theaterOptional.isEmpty()) {
                log.warn("Theater with ID {} not found in DB during update", theaterId);
                return;
            }

            com.example.TicketFlix.Models.Theater theater = theaterOptional.get();
            
            // Update fields
            if (theaterEntryDTO.getName() != null && !theaterEntryDTO.getName().isEmpty()) {
                theater.setName(theaterEntryDTO.getName());
            }
            if (theaterEntryDTO.getLocation() != null && !theaterEntryDTO.getLocation().isEmpty()) {
                theater.setLocation(theaterEntryDTO.getLocation());
            }

            theaterRepository.save(theater);
            log.info("Theater updated successfully in DB - ID: {}", theaterId);

            // Invalidate cache
            String cacheKey = "theater::" + theaterId;
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing theater update event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "theater-deletion-events", groupId = "theater-analytics-group")
    public void consumeTheaterDeletionEvent(String message) {
        log.info("Theater deletion event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int theaterId = (Integer) eventData.get("theaterId");

            Optional<com.example.TicketFlix.Models.Theater> theaterOptional = theaterRepository.findById(theaterId);
            if (theaterOptional.isEmpty()) {
                log.warn("Theater with ID {} not found in DB during deletion", theaterId);
                return;
            }

            theaterRepository.deleteById(theaterId);
            log.info("Theater deleted successfully from DB - ID: {}", theaterId);

            // Invalidate cache
            String cacheKey = "theater::" + theaterId;
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing theater deletion event: {}", e.getMessage(), e);
        }
    }

    // ========== SHOW CONSUMERS ==========
    @KafkaListener(topics = "show-creation-events", groupId = "show-analytics-group")
    public void consumeShowCreationEvent(String message) {
        log.info("Show creation event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            @SuppressWarnings("unchecked")
            Map<String, Object> showEntryMap = (Map<String, Object>) eventData.get("showEntryDTO");
            com.example.TicketFlix.EntryDTOs.ShowEntryDTO showEntryDTO = objectMapper.convertValue(showEntryMap, com.example.TicketFlix.EntryDTOs.ShowEntryDTO.class);

            // Create show in DB
            showService.createShowInDB(showEntryDTO);
            log.info("Show created successfully in DB - Movie ID: {}, Theater ID: {}", 
                    showEntryDTO.getMovieId(), showEntryDTO.getTheaterId());

            // Invalidate cache if needed
            String cacheKey = "show::*";
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing show creation event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "show-update-events", groupId = "show-analytics-group")
    public void consumeShowUpdateEvent(String message) {
        log.info("Show update event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int showId = (Integer) eventData.get("showId");

            @SuppressWarnings("unchecked")
            Map<String, Object> showEntryMap = (Map<String, Object>) eventData.get("showEntryDTO");
            com.example.TicketFlix.EntryDTOs.ShowEntryDTO showEntryDTO = objectMapper.convertValue(showEntryMap, com.example.TicketFlix.EntryDTOs.ShowEntryDTO.class);

            Optional<com.example.TicketFlix.Models.Show> showOptional = showRepository.findById(showId);
            if (showOptional.isEmpty()) {
                log.warn("Show with ID {} not found in DB during update", showId);
                return;
            }

            com.example.TicketFlix.Models.Show show = showOptional.get();
            
            // Update fields
            if (showEntryDTO.getLocalDate() != null) {
                show.setShowDate(showEntryDTO.getLocalDate());
            }
            if (showEntryDTO.getLocalTime() != null) {
                show.setShowTime(showEntryDTO.getLocalTime());
            }
            if (showEntryDTO.getShowType() != null) {
                show.setShowType(showEntryDTO.getShowType());
            }

            showRepository.save(show);
            log.info("Show updated successfully in DB - ID: {}", showId);

            // Invalidate cache
            String cacheKey = "show::" + showId;
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing show update event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "show-deletion-events", groupId = "show-analytics-group")
    public void consumeShowDeletionEvent(String message) {
        log.info("Show deletion event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int showId = (Integer) eventData.get("showId");

            Optional<com.example.TicketFlix.Models.Show> showOptional = showRepository.findById(showId);
            if (showOptional.isEmpty()) {
                log.warn("Show with ID {} not found in DB during deletion", showId);
                return;
            }

            showRepository.deleteById(showId);
            log.info("Show deleted successfully from DB - ID: {}", showId);

            // Invalidate cache
            String cacheKey = "show::" + showId;
            redisTemplate.delete(cacheKey);
            
        } catch (Exception e) {
            log.error("Error processing show deletion event: {}", e.getMessage(), e);
        }
    }

    // ========== TICKET CONSUMERS ==========
    @KafkaListener(topics = "ticket-booking-requests", groupId = "ticket-booking-group")
    public void consumeTicketBookingRequest(String message) {
        log.info("Ticket booking request received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            @SuppressWarnings("unchecked")
            Map<String, Object> ticketEntryMap = (Map<String, Object>) eventData.get("ticketEntryDTO");
            TicketEntryDTO ticketEntryDTO = objectMapper.convertValue(ticketEntryMap, TicketEntryDTO.class);

            Boolean isValidRequest = (Boolean) eventData.get("isValidRequest");
            if (isValidRequest == null || !isValidRequest) {
                log.warn("Ticket booking request was invalid, skipping DB operation");
                return;
            }

            // Create ticket in DB (this includes seat locking, validation, and save)
            ticketService.createTicketInDB(ticketEntryDTO);
            log.info("Ticket created successfully in DB for user ID: {}, show ID: {}", 
                    ticketEntryDTO.getUserId(), ticketEntryDTO.getShowId());
            
        } catch (Exception e) {
            log.error("Error processing ticket booking request: {}", e.getMessage(), e);
            // In production, consider sending to Dead Letter Queue (DLQ) for retry
        }
    }

    @KafkaListener(topics = "ticket-cancellation-requests", groupId = "ticket-cancellation-group")
    public void consumeTicketCancellationRequest(String message) {
        log.info("Ticket cancellation request received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);
            int ticketId = (Integer) eventData.get("ticketId");

            // Cancel ticket in DB
            ticketService.cancelTicketInDB(ticketId);
            log.info("Ticket cancelled successfully in DB - Ticket ID: {}", ticketId);
            
        } catch (Exception e) {
            log.error("Error processing ticket cancellation request: {}", e.getMessage(), e);
            // In production, consider sending to Dead Letter Queue (DLQ) for retry
        }
    }

    // ========== PAYMENT CONSUMERS ==========
    
    @Autowired
    private com.example.TicketFlix.Service.PaymentService paymentService;

    @KafkaListener(topics = "payment-requests", groupId = "payment-processing-group")
    public void consumePaymentRequest(String message) {
        log.info("Payment request received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            @SuppressWarnings("unchecked")
            Map<String, Object> paymentRequestMap = (Map<String, Object>) eventData.get("paymentRequestDTO");
            com.example.TicketFlix.EntryDTOs.PaymentRequestDTO paymentRequestDTO = 
                    objectMapper.convertValue(paymentRequestMap, com.example.TicketFlix.EntryDTOs.PaymentRequestDTO.class);

            String paymentId = (String) eventData.get("paymentId");
            String idempotencyKey = (String) eventData.get("idempotencyKey");

            // Process payment in DB (simulates payment gateway)
            paymentService.processPaymentInDB(paymentId, paymentRequestDTO);
            log.info("Payment processed successfully for payment ID: {}, idempotency key: {}", 
                    paymentId, idempotencyKey);
            
        } catch (Exception e) {
            log.error("Error processing payment request: {}", e.getMessage(), e);
            // In production, consider sending to Dead Letter Queue (DLQ) for retry
        }
    }

    @KafkaListener(topics = "payment-result-events", groupId = "payment-events-group")
    public void consumePaymentResultEvent(String message) {
        log.info("Payment result event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            String paymentId = (String) eventData.get("paymentId");
            String status = (String) eventData.get("status");
            Boolean success = (Boolean) eventData.get("success");

            log.info("Payment result - ID: {}, Status: {}, Success: {}", paymentId, status, success);
            
            // Additional processing can be added here:
            // - Send payment confirmation emails
            // - Update analytics
            // - Trigger refunds if needed
            // - Notify external systems
            
        } catch (Exception e) {
            log.error("Error processing payment result event: {}", e.getMessage(), e);
        }
    }

    // ========== SCREEN CONSUMERS ==========
    
    @Autowired
    private com.example.TicketFlix.Service.ScreenService screenService;

    @KafkaListener(topics = "screen-creation-events", groupId = "screen-creation-group")
    public void consumeScreenCreationEvent(String message) {
        log.info("Screen creation event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            @SuppressWarnings("unchecked")
            Map<String, Object> screenEntryMap = (Map<String, Object>) eventData.get("screenEntryDTO");
            com.example.TicketFlix.EntryDTOs.ScreenEntryDTO screenEntryDTO = 
                    objectMapper.convertValue(screenEntryMap, com.example.TicketFlix.EntryDTOs.ScreenEntryDTO.class);

            // Create screen in DB
            screenService.createScreenInDB(screenEntryDTO);
            log.info("Screen created successfully in DB for theater ID: {}, screen number: {}", 
                    screenEntryDTO.getTheaterId(), screenEntryDTO.getScreenNumber());
            
        } catch (Exception e) {
            log.error("Error processing screen creation event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "screen-update-events", groupId = "screen-update-group")
    public void consumeScreenUpdateEvent(String message) {
        log.info("Screen update event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            Integer screenId = (Integer) eventData.get("screenId");
            @SuppressWarnings("unchecked")
            Map<String, Object> screenEntryMap = (Map<String, Object>) eventData.get("screenEntryDTO");
            com.example.TicketFlix.EntryDTOs.ScreenEntryDTO screenEntryDTO = 
                    objectMapper.convertValue(screenEntryMap, com.example.TicketFlix.EntryDTOs.ScreenEntryDTO.class);

            // Update screen in DB
            screenService.updateScreenInDB(screenId, screenEntryDTO);
            log.info("Screen updated successfully in DB for screen ID: {}", screenId);
            
        } catch (Exception e) {
            log.error("Error processing screen update event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "screen-deletion-events", groupId = "screen-deletion-group")
    public void consumeScreenDeletionEvent(String message) {
        log.info("Screen deletion event received: {}", message);
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> eventData = objectMapper.readValue(message, typeRef);

            Integer screenId = (Integer) eventData.get("screenId");

            // Delete screen from DB
            screenService.deleteScreenInDB(screenId);
            log.info("Screen deleted successfully from DB for screen ID: {}", screenId);
            
        } catch (Exception e) {
            log.error("Error processing screen deletion event: {}", e.getMessage(), e);
        }
    }
}

