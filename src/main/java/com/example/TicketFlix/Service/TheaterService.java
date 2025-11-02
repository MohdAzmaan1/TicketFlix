package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.TheaterConvertor;
import com.example.TicketFlix.EntryDTOs.TheaterEntryDTO;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.Theater;
import com.example.TicketFlix.Repository.TheaterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TheaterService {

    @Autowired
    TheaterRepository theaterRepository;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    KafkaProducerService kafkaProducerService;

    private static final String THEATER_CACHE_KEY = "theater::";
    private static final int CACHE_TTL_HOURS = 24;

    public String addTheater(TheaterEntryDTO theaterEntryDTO) throws Exception {
        // Publish to Kafka (consumer will handle DB persistence)
        kafkaProducerService.publishTheaterCreationEvent(theaterEntryDTO);
        log.info("Theater creation event published to Kafka for: {}", theaterEntryDTO.getName());
        return "Theater creation request submitted successfully";
    }

    public String updateTheater(int theaterId, TheaterEntryDTO theaterEntryDTO) throws Exception {
        Optional<Theater> theaterOptional = theaterRepository.findById(theaterId);
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + theaterId);
        }

        // Publish to Kafka
        kafkaProducerService.publishTheaterUpdateEvent(theaterId, theaterEntryDTO);
        log.info("Theater update event published to Kafka for theater ID: {}", theaterId);
        return "Theater update request submitted successfully";
    }

    public String deleteTheater(int theaterId) throws Exception {
        Optional<Theater> theaterOptional = theaterRepository.findById(theaterId);
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + theaterId);
        }

        // Publish to Kafka
        kafkaProducerService.publishTheaterDeletionEvent(theaterId);
        log.info("Theater deletion event published to Kafka for theater ID: {}", theaterId);
        return "Theater deletion request submitted successfully";
    }

    public List<TheaterEntryDTO> getAllTheaters() throws Exception {
        List<Theater> theaters = theaterRepository.findAll();
        // Optimized: Use Stream API for cleaner code
        return theaters.stream()
                .map(TheaterConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public TheaterEntryDTO getTheaterById(int theaterId) throws Exception {
        // Check Redis cache first
        String cacheKey = THEATER_CACHE_KEY + theaterId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedData != null) {
            log.info("Theater found in cache for ID: {}", theaterId);
        }

        Optional<Theater> theaterOptional = theaterRepository.findById(theaterId);
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + theaterId);
        }

        Theater theater = theaterOptional.get();
        TheaterEntryDTO theaterEntryDTO = TheaterConvertor.convertEntityToDto(theater);

        // Cache the theater data
        try {
            redisTemplate.opsForValue().set(cacheKey, theater.getName(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache theater data: {}", e.getMessage());
        }

        return theaterEntryDTO;
    }

    // This method is called by Kafka consumer to actually create theater
    @org.springframework.transaction.annotation.Transactional
    public String createTheaterInDB(TheaterEntryDTO theaterEntryDTO) {
        Theater theater = TheaterConvertor.convertDtoToEntity(theaterEntryDTO);
        theaterRepository.save(theater);
        
        // If screens are provided, create them (seats will be created with screens)
        if (theaterEntryDTO.getScreens() != null && !theaterEntryDTO.getScreens().isEmpty()) {
            log.info("Creating {} screens for theater {}", theaterEntryDTO.getScreens().size(), theater.getName());
            // Screens will be created via separate screen creation endpoints
            // or can be processed here if needed in future
        }
        
        // Backward compatibility: If old style seats count provided (deprecated), create a default screen
        if (theaterEntryDTO.getClassicSeatsCount() > 0 || theaterEntryDTO.getPremiumSeatsCount() > 0) {
            log.warn("Deprecated: Using classicSeatsCount/premiumSeatsCount. Please use screens instead.");
            // In production, you might want to create a default screen with these seats
            // For now, we'll just log a warning
        }
        
        return "Theater added Successfully. Please add screens using /screens/add endpoint.";
    }
}
