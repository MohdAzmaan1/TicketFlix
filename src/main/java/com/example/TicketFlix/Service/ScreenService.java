package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.ScreenConvertor;
import com.example.TicketFlix.EntryDTOs.ScreenEntryDTO;
import com.example.TicketFlix.Genres.SeatType;
import com.example.TicketFlix.Models.Screen;
import com.example.TicketFlix.Models.Theater;
import com.example.TicketFlix.Models.TheaterSeats;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Repository.ScreenRepository;
import com.example.TicketFlix.Repository.TheaterRepository;
import com.example.TicketFlix.Response.ScreenResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScreenService {

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private static final String SCREEN_CACHE_KEY = "screen::";
    private static final int CACHE_TTL_HOURS = 24;

    /**
     * Add a new screen to a theater
     */
    public String addScreen(ScreenEntryDTO screenEntryDTO) throws Exception {
        // Validate theater exists
        Optional<Theater> theaterOptional = theaterRepository.findById(screenEntryDTO.getTheaterId());
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + screenEntryDTO.getTheaterId());
        }

        // Check if screen number already exists in this theater
        Optional<Screen> existingScreen = screenRepository.findByTheaterIdAndScreenNumber(
                screenEntryDTO.getTheaterId(), screenEntryDTO.getScreenNumber());
        if (existingScreen.isPresent()) {
            throw new Exception("Screen number " + screenEntryDTO.getScreenNumber() + 
                    " already exists in this theater");
        }

        // Publish to Kafka (consumer will handle DB persistence)
        kafkaProducerService.publishScreenCreationEvent(screenEntryDTO);
        log.info("Screen creation event published to Kafka for theater ID: {}, screen number: {}", 
                screenEntryDTO.getTheaterId(), screenEntryDTO.getScreenNumber());
        return "Screen creation request submitted successfully";
    }

    /**
     * Create screen in DB (called by Kafka consumer)
     */
    @Transactional
    public String createScreenInDB(ScreenEntryDTO screenEntryDTO) {
        Optional<Theater> theaterOptional = theaterRepository.findById(screenEntryDTO.getTheaterId());
        if (theaterOptional.isEmpty()) {
            throw new RuntimeException("Theater not found with id: " + screenEntryDTO.getTheaterId());
        }

        Theater theater = theaterOptional.get();
        Screen screen = ScreenConvertor.convertDtoToEntity(screenEntryDTO);
        screen.setTheater(theater);

        // Create seats for this screen
        List<TheaterSeats> theaterSeatsList = createScreenSeats(screenEntryDTO, screen);
        screen.setTheaterSeatsList(theaterSeatsList);

        screen = screenRepository.save(screen);

        // Update theater's screen list
        List<Screen> screenList = theater.getScreenList();
        screenList.add(screen);
        theater.setScreenList(screenList);
        theaterRepository.save(theater);

        log.info("Screen created successfully - ID: {}, Name: {}, Theater: {}", 
                screen.getId(), screen.getName(), theater.getName());
        return "Screen added successfully";
    }

    /**
     * Create seats for a screen
     */
    private List<TheaterSeats> createScreenSeats(ScreenEntryDTO screenEntryDTO, Screen screen) {
        int numberOfClassicSeats = screenEntryDTO.getClassicSeatsCount();
        int numberOfPremiumSeats = screenEntryDTO.getPremiumSeatsCount();

        List<TheaterSeats> theaterSeatEntityList = new ArrayList<>();

        // Create classic seats
        for (int count = 1; count <= numberOfClassicSeats; count++) {
            TheaterSeats theaterSeat = TheaterSeats.builder()
                    .seatType(SeatType.CLASSIC)
                    .seatNumber("S" + screen.getScreenNumber() + "-" + count + "C")
                    .screen(screen)
                    .build();
            theaterSeatEntityList.add(theaterSeat);
        }

        // Create premium seats
        for (int count = 1; count <= numberOfPremiumSeats; count++) {
            TheaterSeats theaterSeat = TheaterSeats.builder()
                    .seatType(SeatType.PREMIUM)
                    .seatNumber("S" + screen.getScreenNumber() + "-" + count + "P")
                    .screen(screen)
                    .build();
            theaterSeatEntityList.add(theaterSeat);
        }

        return theaterSeatEntityList;
    }

    /**
     * Get all screens
     */
    public List<ScreenResponseDTO> getAllScreens() throws Exception {
        List<Screen> screens = screenRepository.findAll();
        return screens.stream()
                .map(ScreenConvertor::convertEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get screen by ID (with Redis caching)
     */
    public ScreenResponseDTO getScreenById(int screenId) throws Exception {
        // Check Redis cache first
        String cacheKey = SCREEN_CACHE_KEY + screenId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);

        if (cachedData != null) {
            log.info("Screen found in cache for ID: {}", screenId);
        }

        Optional<Screen> screenOptional = screenRepository.findById(screenId);
        if (screenOptional.isEmpty()) {
            throw new Exception("Screen not found with id: " + screenId);
        }

        Screen screen = screenOptional.get();
        ScreenResponseDTO screenResponseDTO = ScreenConvertor.convertEntityToDto(screen);

        // Cache the screen data
        try {
            redisTemplate.opsForValue().set(cacheKey, screen.getName(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache screen data: {}", e.getMessage());
        }

        return screenResponseDTO;
    }

    /**
     * Get all screens by theater ID
     */
    public List<ScreenResponseDTO> getScreensByTheater(int theaterId) throws Exception {
        Optional<Theater> theaterOptional = theaterRepository.findById(theaterId);
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + theaterId);
        }

        List<Screen> screens = screenRepository.findByTheaterId(theaterId);
        return screens.stream()
                .map(ScreenConvertor::convertEntityToDto)
                .collect(Collectors.toList());
    }

    /**
     * Update screen
     */
    public String updateScreen(int screenId, ScreenEntryDTO screenEntryDTO) throws Exception {
        Optional<Screen> screenOptional = screenRepository.findById(screenId);
        if (screenOptional.isEmpty()) {
            throw new Exception("Screen not found with id: " + screenId);
        }

        // Publish to Kafka
        kafkaProducerService.publishScreenUpdateEvent(screenId, screenEntryDTO);
        log.info("Screen update event published to Kafka for screen ID: {}", screenId);
        return "Screen update request submitted successfully";
    }

    /**
     * Update screen in DB (called by Kafka consumer)
     */
    @Transactional
    public String updateScreenInDB(int screenId, ScreenEntryDTO screenEntryDTO) {
        Optional<Screen> screenOptional = screenRepository.findById(screenId);
        if (screenOptional.isEmpty()) {
            throw new RuntimeException("Screen not found with id: " + screenId);
        }

        Screen screen = screenOptional.get();
        screen.setName(screenEntryDTO.getName());
        // Note: Screen number and seats are typically not changed after creation

        screenRepository.save(screen);

        // Invalidate cache
        try {
            redisTemplate.delete(SCREEN_CACHE_KEY + screenId);
        } catch (Exception e) {
            log.warn("Failed to invalidate screen cache: {}", e.getMessage());
        }

        log.info("Screen updated successfully - ID: {}, Name: {}", screen.getId(), screen.getName());
        return "Screen updated successfully";
    }

    /**
     * Delete screen
     */
    public String deleteScreen(int screenId) throws Exception {
        Optional<Screen> screenOptional = screenRepository.findById(screenId);
        if (screenOptional.isEmpty()) {
            throw new Exception("Screen not found with id: " + screenId);
        }

        // Check if screen has shows scheduled
        Screen screen = screenOptional.get();
        if (screen.getShowList() != null && !screen.getShowList().isEmpty()) {
            throw new Exception("Cannot delete screen with scheduled shows. Please cancel all shows first.");
        }

        // Publish to Kafka
        kafkaProducerService.publishScreenDeletionEvent(screenId);
        log.info("Screen deletion event published to Kafka for screen ID: {}", screenId);
        return "Screen deletion request submitted successfully";
    }

    /**
     * Delete screen in DB (called by Kafka consumer)
     */
    @Transactional
    public String deleteScreenInDB(int screenId) {
        Optional<Screen> screenOptional = screenRepository.findById(screenId);
        if (screenOptional.isEmpty()) {
            throw new RuntimeException("Screen not found with id: " + screenId);
        }

        Screen screen = screenOptional.get();

        // Verify no shows scheduled
        if (screen.getShowList() != null && !screen.getShowList().isEmpty()) {
            throw new RuntimeException("Cannot delete screen with scheduled shows");
        }

        // Remove from theater's screen list
        Theater theater = screen.getTheater();
        if (theater != null) {
            List<Screen> screenList = theater.getScreenList();
            screenList.remove(screen);
            theater.setScreenList(screenList);
            theaterRepository.save(theater);
        }

        screenRepository.delete(screen);

        // Invalidate cache
        try {
            redisTemplate.delete(SCREEN_CACHE_KEY + screenId);
        } catch (Exception e) {
            log.warn("Failed to invalidate screen cache: {}", e.getMessage());
        }

        log.info("Screen deleted successfully - ID: {}", screenId);
        return "Screen deleted successfully";
    }
}

