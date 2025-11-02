package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.TicketConvertor;
import com.example.TicketFlix.EntryDTOs.DeleteTicketEntryDTO;
import com.example.TicketFlix.EntryDTOs.TicketEntryDTO;
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
import javax.mail.MessagingException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    ShowRepository showRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RedisService redisService;

    @Autowired
    KafkaProducerService kafkaProducerService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    private static final String TICKET_CACHE_KEY = "ticket::";
    private static final String USER_TICKETS_CACHE_KEY = "user-tickets::";
    private static final String SHOW_TICKETS_CACHE_KEY = "show-tickets::";
    private static final int CACHE_TTL_HOURS = 12;

    public String addTicket(TicketEntryDTO ticketEntryDTO) throws InterruptedException, MessagingException {
        // Validate seats and acquire locks BEFORE publishing to Kafka
        List<String> requestedSeatsToBook = ticketEntryDTO.getRequestedSeats();
        List<RLock> locks = new ArrayList<>();
        boolean allLocksAcquired = false;
        
        try {
            // Acquire locks for all requested seats
            for(String seatNumber: requestedSeatsToBook){
                String lockKey = "seat-lock" + "-" + ticketEntryDTO.getShowId() + "-"+ seatNumber;
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = lock.tryLock(5, 3, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new InterruptedException("Seat " + seatNumber + " is currently being booked by another user. Please try again.");
                }
                locks.add(lock);
            }
            allLocksAcquired = true;

            // Validation : Check if the requested seats are available or not ?
            boolean isValidRequest = checkValidityOfRequestedSeats(ticketEntryDTO);

            if (!isValidRequest) {
                log.error("Requested seats are not available");
                throw new MessagingException("Requested seats are not available");
            }

            // Publish to Kafka (consumer will handle DB persistence)
            // The locks will be released after publishing, consumer needs to re-acquire them
            kafkaProducerService.publishTicketBookingRequest(ticketEntryDTO, isValidRequest);
            log.info("Ticket booking request published to Kafka for user ID: {}, show ID: {}", 
                    ticketEntryDTO.getUserId(), ticketEntryDTO.getShowId());
            
            return "Ticket booking request submitted successfully. Confirmation email will be sent shortly.";
        } finally {
            if(allLocksAcquired){
                // Release locks after publishing to Kafka
                for(int i=0; i < locks.size(); i++){
                    try {
                        locks.get(i).unlock();
                    } catch (Exception e) {
                        log.error("Failed to release lock for seat: " + requestedSeatsToBook.get(i), e);
                    }
                }
            }
        }
    }

    public String cancelTicket(DeleteTicketEntryDTO deleteTicketEntryDTO) throws Exception {
        // Validate ticket exists before publishing to Kafka
        Optional<Ticket> ticketOptional = ticketRepository.findById(deleteTicketEntryDTO.getTicketId());
        if (ticketOptional.isEmpty()) {
            throw new Exception("Ticket not found with id: " + deleteTicketEntryDTO.getTicketId());
        }

        // Publish to Kafka (consumer will handle DB cancellation)
        kafkaProducerService.publishTicketCancellationRequest(deleteTicketEntryDTO.getTicketId());
        log.info("Ticket cancellation request published to Kafka for ticket ID: {}", deleteTicketEntryDTO.getTicketId());
        
        return "Ticket cancellation request submitted successfully. Confirmation email will be sent shortly.";
    }

    // New GET APIs with Redis caching
    public List<TicketResponseDTO> getAllTickets() throws Exception {
        List<Ticket> tickets = ticketRepository.findAll();
        // Optimized: Use Stream API for cleaner code
        return tickets.stream()
                .map(TicketConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public TicketResponseDTO getTicketById(int ticketId) throws Exception {
        // Check Redis cache first
        String cacheKey = TICKET_CACHE_KEY + ticketId;
        String cachedTicketId = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedTicketId != null) {
            log.info("Ticket found in cache for ID: {}", ticketId);
        }

        Optional<Ticket> ticketOptional = ticketRepository.findById(ticketId);
        if (ticketOptional.isEmpty()) {
            throw new Exception("Ticket not found with id: " + ticketId);
        }

        Ticket ticket = ticketOptional.get();
        TicketResponseDTO ticketResponseDTO = TicketConvertor.convertEntityToDto(ticket);

        // Cache the ticket data
        try {
            redisTemplate.opsForValue().set(cacheKey, ticket.getTicketId(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache ticket data: {}", e.getMessage());
        }

        return ticketResponseDTO;
    }

    public TicketResponseDTO getTicketByTicketId(String ticketId) throws Exception {
        Optional<Ticket> ticketOptional = ticketRepository.findByTicketId(ticketId);
        if (ticketOptional.isEmpty()) {
            throw new Exception("Ticket not found with ticketId: " + ticketId);
        }

        Ticket ticket = ticketOptional.get();
        return TicketConvertor.convertEntityToDto(ticket);
    }

    public List<TicketResponseDTO> getTicketsByUser(int userId) throws Exception {
        // Check Redis cache first
        String cacheKey = USER_TICKETS_CACHE_KEY + userId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedData != null) {
            log.info("User tickets found in cache for user ID: {}", userId);
        }

        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        // Optimized: Use Stream API for cleaner code
        List<TicketResponseDTO> ticketResponseDTOList = tickets.stream()
                .map(TicketConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());

        // Cache the count of tickets
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(tickets.size()), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache user tickets data: {}", e.getMessage());
        }

        return ticketResponseDTOList;
    }

    public List<TicketResponseDTO> getTicketsByShow(int showId) throws Exception {
        // Check Redis cache first
        String cacheKey = SHOW_TICKETS_CACHE_KEY + showId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedData != null) {
            log.info("Show tickets found in cache for show ID: {}", showId);
        }

        List<Ticket> tickets = ticketRepository.findByShowId(showId);
        // Optimized: Use Stream API for cleaner code
        List<TicketResponseDTO> ticketResponseDTOList = tickets.stream()
                .map(TicketConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());

        // Cache the count of tickets
        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(tickets.size()), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache show tickets data: {}", e.getMessage());
        }

        return ticketResponseDTOList;
    }

    // ========== Methods called by Kafka Consumer ==========
    
    // This method is called by Kafka consumer to actually create ticket in DB
    @Transactional
    public String createTicketInDB(TicketEntryDTO ticketEntryDTO) throws InterruptedException, MessagingException {
        List<String> requestedSeatsToBook = ticketEntryDTO.getRequestedSeats();
        List<RLock> locks = new ArrayList<>();
        boolean allLocksAcquired = false;
        
        try {
            // Re-acquire locks for all requested seats
            for(String seatNumber: requestedSeatsToBook){
                String lockKey = "seat-lock" + "-" + ticketEntryDTO.getShowId() + "-"+ seatNumber;
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = lock.tryLock(10, 3, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new InterruptedException("Seat " + seatNumber + " is currently being booked by another user.");
                }
                locks.add(lock);
            }
            allLocksAcquired = true;

            // Re-validate seats (in case they were booked between request and processing)
            boolean isValidRequest = checkValidityOfRequestedSeats(ticketEntryDTO);
            if (!isValidRequest) {
                throw new MessagingException("Requested seats are no longer available");
            }

            Optional<Show> showOptional = showRepository.findById(ticketEntryDTO.getShowId());
            if (showOptional.isEmpty()) {
                throw new MessagingException("Show not found with id: " + ticketEntryDTO.getShowId());
            }
            
            Show show = showOptional.get();
            
            // Validate movie and theater exist
            if (show.getMovie() == null) {
                throw new MessagingException("Movie not found for show");
            }
            if (show.getTheater() == null) {
                throw new MessagingException("Theater not found for show");
            }
            
            List<ShowSeat> seatList = show.getListOfShowSeats();
            Set<String> requestedSeatsSet = new HashSet<>(ticketEntryDTO.getRequestedSeats());
            List<String> requestedSeats = ticketEntryDTO.getRequestedSeats();

            // Calculate the total amount and mark seats as booked
            int totalAmount = 0;
            for (ShowSeat showSeat : seatList) {
                if (requestedSeatsSet.contains(showSeat.getSeatNumber())) {
                    totalAmount += showSeat.getPrice();
                    showSeat.setBooked(true);
                    showSeat.setBookedAt(new Date());
                }
            }

            // Validate user exists
            Optional<User> userOptional = userRepository.findById(ticketEntryDTO.getUserId());
            if (userOptional.isEmpty()) {
                throw new MessagingException("User not found with id: " + ticketEntryDTO.getUserId());
            }

            // Create ticket entity
            Ticket ticket = Ticket.builder()
                    .totalAmount(totalAmount)
                    .movieName(show.getMovie().getMovieName())
                    .showDate(show.getShowDate())
                    .showTime(show.getShowTime())
                    .theaterName(show.getTheater().getName())
                    .bookedSeat(getAllowedSeatsFromShowSeats(requestedSeats))
                    .user(userOptional.get())
                    .show(show)
                    .build();

            ticket = ticketRepository.save(ticket);

            // Update parent entities
            List<Ticket> ticketList = show.getListOfBookedTickets();
            ticketList.add(ticket);
            show.setListOfBookedTickets(ticketList);
            showRepository.save(show);

            User user = ticket.getUser();
            List<Ticket> ticketList1 = user.getBookedTickets();
            ticketList1.add(ticket);
            user.setBookedTickets(ticketList1);
            userRepository.save(user);

            // Update trending movies in Redis
            redisService.increaseMovieCounter(ticket.getMovieName());
            
            // Publish analytics event
            kafkaProducerService.publishTicketBookingEvent(ticket, user);
            
            // Send email notification
            String subject = "Confirmation for your ticket booking";
            String body = "Hi, " + user.getName() + "\n\nThis is to confirm your ticket booking for the movie:- " + ticket.getMovieName()
                    + "\nTicket id - " + ticket.getTicketId() + "\nBooked Seats - " + ticket.getBookedSeat() + "\nAmount of rupees - "
                    + ticket.getTotalAmount() + "\n\n\n" + "Thank you for using our services, have a wonderful day!";
            kafkaProducerService.publishEmailNotification(user.getEmail(), subject, body);
            
            // Invalidate relevant caches
            invalidateTicketCaches(ticket);

            log.info("Ticket created successfully in DB - ID: {}, Ticket ID: {}", ticket.getId(), ticket.getTicketId());
            return "Ticket is successfully booked. Confirmation email will be sent shortly.";

        } finally {
            if(allLocksAcquired){
                for(int i=0; i < locks.size(); i++){
                    try {
                        locks.get(i).unlock();
                    } catch (Exception e) {
                        log.error("Failed to release lock for seat: " + requestedSeatsToBook.get(i), e);
                    }
                }
            }
        }
    }

    // This method is called by Kafka consumer to actually cancel ticket in DB
    @Transactional
    public String cancelTicketInDB(int ticketId) throws Exception {
        Optional<Ticket> ticketOptional = ticketRepository.findById(ticketId);
        if (ticketOptional.isEmpty()) {
            throw new Exception("Ticket not found with id: " + ticketId);
        }

        Ticket ticket = ticketOptional.get();
        String ticketsToBeDeleted = ticket.getBookedSeat();

        String[] currSeats = ticketsToBeDeleted.split(",");
        String cancelledSeats = String.join(",", currSeats);

        Show show = ticket.getShow();
        List<ShowSeat> showSeatList = show.getListOfShowSeats();

        cancelBookingOfSeats(currSeats, showSeatList);

        showRepository.save(show);

        User user = ticket.getUser();
        redisService.decreaseCounter(ticket.getMovieName());
        
        // Publish analytics event
        kafkaProducerService.publishTicketCancellationEvent(ticket, user);
        
        // Send cancellation email
        String subject = "Confirmation for your ticket cancellation";
        String body = "Hi, " + user.getName() + "\n\nThis is to confirm your booking cancellation.\n"
                + "Ticket id - " + ticket.getTicketId() + "\nCancelled Seats - " + cancelledSeats + "\n"
                + "Amount of rupees - " + ticket.getTotalAmount() + " will be refunded in to your account in 6-7 working days"
                + "\n\n\n" + "Have a wonderful day!";
        kafkaProducerService.publishEmailNotification(user.getEmail(), subject, body);
        
        // Invalidate relevant caches
        invalidateTicketCaches(ticket);

        // Optionally delete the ticket or mark it as cancelled
        // For now, we'll keep it for audit purposes
        log.info("Ticket cancelled successfully in DB - Ticket ID: {}", ticket.getTicketId());
        return "Tickets have been successfully cancelled. Confirmation email will be sent shortly.";
    }

    private void invalidateTicketCaches(Ticket ticket) {
        try {
            // Invalidate ticket cache
            redisTemplate.delete(TICKET_CACHE_KEY + ticket.getId());
            // Invalidate user tickets cache
            if (ticket.getUser() != null) {
                redisTemplate.delete(USER_TICKETS_CACHE_KEY + ticket.getUser().getId());
            }
            // Invalidate show tickets cache
            if (ticket.getShow() != null) {
                redisTemplate.delete(SHOW_TICKETS_CACHE_KEY + ticket.getShow().getId());
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate ticket caches: {}", e.getMessage());
        }
    }

    private String getAllowedSeatsFromShowSeats(List<String> requestedSeats){
        // Optimized: Use String.join instead of loop concatenation
        return String.join(",", requestedSeats);
    }

    private boolean checkValidityOfRequestedSeats(TicketEntryDTO ticketEntryDTO){
        int showId = ticketEntryDTO.getShowId();
        List<String> requestedSeats = ticketEntryDTO.getRequestedSeats();
        
        Optional<Show> showOptional = showRepository.findById(showId);
        if (showOptional.isEmpty()) {
            log.error("Show not found with ID: {}", showId);
            return false;
        }
        
        Show show = showOptional.get();
        List<ShowSeat> listOfSeats = show.getListOfShowSeats();

        // Optimized: Use Set for O(1) lookup instead of List.contains() which is O(n)
        Set<String> requestedSeatsSet = new HashSet<>(requestedSeats);
        
        for(ShowSeat showSeat : listOfSeats){
            String seatNo = showSeat.getSeatNumber();
            if(requestedSeatsSet.contains(seatNo)){
                if(showSeat.isBooked()){
                    return false;
                }
            }
        }
        return true;
    }

    private void cancelBookingOfSeats(String[] currSeats, List<ShowSeat> showSeatList) {
        // Optimized: Use Set for O(1) lookup instead of Arrays.asList().contains() which is O(n)
        Set<String> seatsToCancel = new HashSet<>(Arrays.asList(currSeats));
        for(ShowSeat showSeat : showSeatList){
            if(seatsToCancel.contains(showSeat.getSeatNumber())){
                showSeat.setBookedAt(null);
                showSeat.setBooked(false);
            }
        }
    }
}
