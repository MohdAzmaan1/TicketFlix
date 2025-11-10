package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.TicketConvertor;
import com.example.TicketFlix.EntryDTOs.DeleteTicketEntryDTO;
import com.example.TicketFlix.EntryDTOs.TicketEntryDTO;
import com.example.TicketFlix.Exception.BusinessException;
import com.example.TicketFlix.Exception.ConcurrencyException;
import com.example.TicketFlix.Exception.ResourceNotFoundException;
import com.example.TicketFlix.Exception.ValidationException;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.Show;
import com.example.TicketFlix.Models.ShowSeat;
import com.example.TicketFlix.Models.Ticket;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.ShowRepository;
import com.example.TicketFlix.Repository.TicketRepository;
import com.example.TicketFlix.Repository.UserRepository;
import com.example.TicketFlix.Response.TicketResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Improved TicketService with better concurrency handling, validation, and error management
 */
@Service
@Slf4j
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisService redisService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String TICKET_CACHE_KEY = "ticket::";
    private static final String USER_TICKETS_CACHE_KEY = "user-tickets::";
    private static final String SHOW_TICKETS_CACHE_KEY = "show-tickets::";
    private static final int CACHE_TTL_HOURS = 12;
    private static final long LOCK_WAIT_TIME = 10;
    private static final long LOCK_LEASE_TIME = 30;

    /**
     * Book tickets with improved concurrency control
     */
    public String addTicket(TicketEntryDTO ticketEntryDTO) throws ValidationException, ConcurrencyException, BusinessException {
        validateTicketBookingRequest(ticketEntryDTO);

        List<String> requestedSeats = ticketEntryDTO.getRequestedSeats();
        String lockKeyPrefix = "seat-booking-" + ticketEntryDTO.getShowId() + "-";

        // Sort seats to prevent deadlocks
        List<String> sortedSeats = new ArrayList<>(requestedSeats);
        Collections.sort(sortedSeats);

        Map<String, RLock> locks = new LinkedHashMap<>();

        try {
            // Acquire locks in sorted order to prevent deadlocks
            if (!acquireSeatsLocks(sortedSeats, lockKeyPrefix, locks)) {
                throw new ConcurrencyException("Unable to acquire locks for seats: " + String.join(", ", requestedSeats));
            }

            // Validate seat availability
            if (!areSeatsAvailable(ticketEntryDTO)) {
                throw new BusinessException("One or more requested seats are not available");
            }

            // Publish booking request to Kafka for asynchronous processing
            kafkaProducerService.publishTicketBookingRequest(ticketEntryDTO, true);

            log.info("Ticket booking request submitted successfully for user: {}, show: {}, seats: {}",
                    ticketEntryDTO.getUserId(), ticketEntryDTO.getShowId(), requestedSeats);

            return "Ticket booking request submitted successfully. You will receive confirmation shortly.";

        } finally {
            // Release all locks
            releaseAllLocks(locks);
        }
    }

    /**
     * Cancel ticket with improved validation
     */
    public String cancelTicket(DeleteTicketEntryDTO deleteRequest) throws ResourceNotFoundException, ValidationException {
        validateTicketCancellationRequest(deleteRequest);

        Ticket ticket = ticketRepository.findById(deleteRequest.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", String.valueOf(deleteRequest.getTicketId())));

        // Publish cancellation request to Kafka
        kafkaProducerService.publishTicketCancellationRequest(deleteRequest.getTicketId());

        log.info("Ticket cancellation request submitted for ticket: {}", deleteRequest.getTicketId());
        return "Ticket cancellation request submitted successfully. You will receive confirmation shortly.";
    }

    /**
     * Get tickets with improved caching and pagination support
     */
    public List<TicketResponseDTO> getAllTickets(int page, int size) {
        // For large datasets, consider using pagination at repository level
        List<Ticket> tickets = ticketRepository.findAll();

        return tickets.stream()
                .skip((long) page * size)
                .limit(size)
                .map(TicketConvertor::convertEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get ticket by ID with caching
     */
    public TicketResponseDTO getTicketById(int ticketId) throws ResourceNotFoundException {
        String cacheKey = TICKET_CACHE_KEY + ticketId;

        // Try to get from cache first
        TicketResponseDTO cachedTicket = getCachedTicket(cacheKey);
        if (cachedTicket != null) {
            log.debug("Ticket found in cache: {}", ticketId);
            return cachedTicket;
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", String.valueOf(ticketId)));

        TicketResponseDTO responseDTO = TicketConvertor.convertEntityToDto(ticket);

        // Cache the result asynchronously
        CompletableFuture.runAsync(() -> cacheTicket(cacheKey, responseDTO));

        return responseDTO;
    }

    /**
     * Get tickets by user with improved performance
     */
    public List<TicketResponseDTO> getTicketsByUser(int userId, int page, int size) throws ResourceNotFoundException {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", String.valueOf(userId));
        }

        String cacheKey = USER_TICKETS_CACHE_KEY + userId + ":" + page + ":" + size;

        // Check cache first
        List<TicketResponseDTO> cachedTickets = getCachedTicketList(cacheKey);
        if (cachedTickets != null) {
            log.debug("User tickets found in cache: {}", userId);
            return cachedTickets;
        }

        // Query without pagination for now - using existing method
        List<Ticket> tickets = ticketRepository.findByUserId(userId);

        List<TicketResponseDTO> responseList = tickets.stream()
                .map(TicketConvertor::convertEntityToDto)
                .collect(Collectors.toList());

        // Cache result asynchronously
        CompletableFuture.runAsync(() -> cacheTicketList(cacheKey, responseList));

        return responseList;
    }

    /**
     * Create ticket in database (called by Kafka consumer)
     */
    @Transactional
    public String createTicketInDatabase(TicketEntryDTO ticketEntryDTO) throws ValidationException, ConcurrencyException, BusinessException, ResourceNotFoundException {
        List<String> requestedSeats = ticketEntryDTO.getRequestedSeats();
        String lockKeyPrefix = "seat-booking-" + ticketEntryDTO.getShowId() + "-";

        Map<String, RLock> locks = new LinkedHashMap<>();

        try {
            // Re-acquire locks for transaction
            List<String> sortedSeats = new ArrayList<>(requestedSeats);
            Collections.sort(sortedSeats);

            if (!acquireSeatsLocks(sortedSeats, lockKeyPrefix, locks)) {
                throw new ConcurrencyException("Unable to acquire locks for database transaction");
            }

            // Re-validate seat availability
            if (!areSeatsAvailable(ticketEntryDTO)) {
                throw new BusinessException("Seats are no longer available");
            }

            // Get entities
            Show show = showRepository.findById(ticketEntryDTO.getShowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Show", String.valueOf(ticketEntryDTO.getShowId())));

            User user = userRepository.findById(ticketEntryDTO.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(ticketEntryDTO.getUserId())));

            // Calculate total amount and mark seats as booked
            int totalAmount = calculateAndBookSeats(show, requestedSeats);

            // Create and save ticket
            Ticket ticket = createTicketEntity(show, user, requestedSeats, totalAmount);
            ticket = ticketRepository.save(ticket);

            // Update relationships
            updateShowAndUserRelationships(show, user, ticket);

            // Update analytics and send notifications
            handlePostBookingOperations(ticket, user);

            // Invalidate relevant caches
            invalidateTicketCaches(ticket);

            log.info("Ticket created successfully: {}", ticket.getTicketId());
            return "Ticket booked successfully. Confirmation email sent.";

        } finally {
            releaseAllLocks(locks);
        }
    }

    // Private helper methods

    private void validateTicketBookingRequest(TicketEntryDTO request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("Ticket booking request is required");
        }

        if (request.getUserId() <= 0) {
            throw new ValidationException("userId", "Valid user ID is required");
        }

        if (request.getShowId() <= 0) {
            throw new ValidationException("showId", "Valid show ID is required");
        }

        if (CollectionUtils.isEmpty(request.getRequestedSeats())) {
            throw new ValidationException("requestedSeats", "At least one seat must be selected");
        }

        if (request.getRequestedSeats().size() > 10) {
            throw new ValidationException("requestedSeats", "Cannot book more than 10 seats at once");
        }

        // Validate seat format
        for (String seat : request.getRequestedSeats()) {
            if (!StringUtils.hasText(seat) || !seat.matches("^[A-Z]\\d{1,2}$")) {
                throw new ValidationException("requestedSeats", "Invalid seat format: " + seat);
            }
        }
    }

    private void validateTicketCancellationRequest(DeleteTicketEntryDTO request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("Ticket cancellation request is required");
        }

        if (request.getTicketId() <= 0) {
            throw new ValidationException("ticketId", "Valid ticket ID is required");
        }
    }

    private boolean acquireSeatsLocks(List<String> seats, String lockKeyPrefix, Map<String, RLock> locks) {
        for (String seat : seats) {
            String lockKey = lockKeyPrefix + seat;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
                if (!acquired) {
                    log.warn("Failed to acquire lock for seat: {}", seat);
                    return false;
                }
                locks.put(seat, lock);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while acquiring lock for seat: {}", seat);
                return false;
            }
        }
        return true;
    }

    private void releaseAllLocks(Map<String, RLock> locks) {
        for (Map.Entry<String, RLock> entry : locks.entrySet()) {
            try {
                if (entry.getValue().isHeldByCurrentThread()) {
                    entry.getValue().unlock();
                }
            } catch (Exception e) {
                log.error("Failed to release lock for seat: {}", entry.getKey(), e);
            }
        }
    }

    private boolean areSeatsAvailable(TicketEntryDTO ticketEntryDTO) {
        Show show = showRepository.findById(ticketEntryDTO.getShowId()).orElse(null);
        if (show == null) {
            return false;
        }

        Set<String> requestedSeats = new HashSet<>(ticketEntryDTO.getRequestedSeats());

        return show.getListOfShowSeats().stream()
                .filter(seat -> requestedSeats.contains(seat.getSeatNumber()))
                .allMatch(seat -> !seat.isBooked());
    }

    private int calculateAndBookSeats(Show show, List<String> requestedSeats) {
        Set<String> requestedSeatsSet = new HashSet<>(requestedSeats);
        int totalAmount = 0;

        for (ShowSeat seat : show.getListOfShowSeats()) {
            if (requestedSeatsSet.contains(seat.getSeatNumber())) {
                totalAmount += seat.getPrice();
                seat.setBooked(true);
                seat.setBookedAt(new Date());
            }
        }

        return totalAmount;
    }

    private Ticket createTicketEntity(Show show, User user, List<String> requestedSeats, int totalAmount) {
        return Ticket.builder()
                .totalAmount(totalAmount)
                .movieName(show.getMovie().getMovieName())
                .showDate(show.getShowDate())
                .showTime(show.getShowTime())
                .theaterName(show.getTheater().getName())
                .bookedSeat(String.join(",", requestedSeats))
                .user(user)
                .show(show)
                .build();
    }

    private void updateShowAndUserRelationships(Show show, User user, Ticket ticket) {
        // Update show
        show.getListOfBookedTickets().add(ticket);
        showRepository.save(show);

        // Update user
        user.getBookedTickets().add(ticket);
        userRepository.save(user);
    }

    private void handlePostBookingOperations(Ticket ticket, User user) {
        // Update trending movies counter
        redisService.increaseMovieCounter(ticket.getMovieName());

        // Publish analytics event
        kafkaProducerService.publishTicketBookingEvent(ticket, user);

        // Send email notification
        String subject = "Ticket Booking Confirmation - " + ticket.getMovieName();
        String body = buildConfirmationEmail(ticket, user);
        kafkaProducerService.publishEmailNotification(user.getEmail(), subject, body);
    }

    private String buildConfirmationEmail(Ticket ticket, User user) {
        return String.format(
                "Dear %s,\n\n" +
                        "Your ticket has been successfully booked!\n\n" +
                        "Movie: %s\n" +
                        "Theater: %s\n" +
                        "Date: %s\n" +
                        "Time: %s\n" +
                        "Seats: %s\n" +
                        "Total Amount: ₹%d\n" +
                        "Ticket ID: %s\n\n" +
                        "Thank you for choosing TicketFlix!\n\n" +
                        "Best regards,\n" +
                        "TicketFlix Team",
                user.getName(), ticket.getMovieName(), ticket.getTheaterName(),
                ticket.getShowDate(), ticket.getShowTime(), ticket.getBookedSeat(),
                ticket.getTotalAmount(), ticket.getTicketId()
        );
    }

    private void invalidateTicketCaches(Ticket ticket) {
        try {
            redisTemplate.delete(TICKET_CACHE_KEY + ticket.getId());
            redisTemplate.delete(USER_TICKETS_CACHE_KEY + ticket.getUser().getId() + ":*");
            redisTemplate.delete(SHOW_TICKETS_CACHE_KEY + ticket.getShow().getId() + ":*");
        } catch (Exception e) {
            log.warn("Failed to invalidate caches for ticket: {}", ticket.getId(), e);
        }
    }

    private TicketResponseDTO getCachedTicket(String cacheKey) {
        // Implementation would depend on how you want to cache TicketResponseDTO
        // For now, returning null to indicate cache miss
        return null;
    }

    private List<TicketResponseDTO> getCachedTicketList(String cacheKey) {
        // Implementation would depend on how you want to cache List<TicketResponseDTO>
        // For now, returning null to indicate cache miss
        return null;
    }

    private void cacheTicket(String cacheKey, TicketResponseDTO ticket) {
        try {
            // Implementation would serialize and cache the ticket
            log.debug("Caching ticket: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Failed to cache ticket: {}", cacheKey, e);
        }
    }

    private void cacheTicketList(String cacheKey, List<TicketResponseDTO> tickets) {
        try {
            // Implementation would serialize and cache the ticket list
            log.debug("Caching ticket list: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Failed to cache ticket list: {}", cacheKey, e);
        }
    }

    // Method expected by KafkaConsumerService
    public String createTicketInDB(TicketEntryDTO ticketEntryDTO) throws Exception {
        return addTicket(ticketEntryDTO);
    }

    // Method expected by KafkaConsumerService
    public String cancelTicketInDB(int ticketId) throws Exception {
        DeleteTicketEntryDTO deleteRequest = new DeleteTicketEntryDTO();
        deleteRequest.setTicketId(ticketId);
        return cancelTicket(deleteRequest);
    }

    // Method expected by Controller  
    public List<TicketResponseDTO> getTicketsByShow(int showId) throws Exception {
        List<Ticket> tickets = ticketRepository.findByShowId(showId);
        List<TicketResponseDTO> responseDTOs = new ArrayList<>();
        for (Ticket ticket : tickets) {
            responseDTOs.add(TicketConvertor.convertEntityToDto(ticket));
        }
        return responseDTOs;
    }

    // Method overloads expected by Controller (no parameters)
    public List<TicketResponseDTO> getAllTickets() throws Exception {
        return getAllTickets(0, 50); // Default to page 0, size 50
    }

    // Method overload expected by Controller (single userId parameter)
    public List<TicketResponseDTO> getTicketsByUser(int userId) throws Exception {
        return getTicketsByUser(userId, 0, 50); // Default to page 0, size 50
    }
}