package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.ShowConvertor;
import com.example.TicketFlix.EntryDTOs.ShowEntryDTO;
import com.example.TicketFlix.Genres.SeatType;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.*;
import com.example.TicketFlix.Repository.MovieRepository;
import com.example.TicketFlix.Repository.ShowRepository;
import com.example.TicketFlix.Repository.TheaterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShowService {

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    TheaterRepository theaterRepository;

    @Autowired
    ShowRepository showRepository;

    @Autowired
    com.example.TicketFlix.Repository.ScreenRepository screenRepository;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    KafkaProducerService kafkaProducerService;

    private static final String SHOW_CACHE_KEY = "show::";
    private static final int CACHE_TTL_HOURS = 24;

    public String addShow(ShowEntryDTO showEntryDTO) throws Exception {
        // Validate movie and theater exist
        Optional<Movie> movieOptional = movieRepository.findById(showEntryDTO.getMovieId());
        if (movieOptional.isEmpty()) {
            throw new Exception("Movie not found with id: " + showEntryDTO.getMovieId());
        }
        
        Optional<Theater> theaterOptional = theaterRepository.findById(showEntryDTO.getTheaterId());
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + showEntryDTO.getTheaterId());
        }

        // Validate screen exists and belongs to theater
        if (showEntryDTO.getScreenId() <= 0) {
            throw new Exception("Screen ID is required");
        }
        
        Optional<com.example.TicketFlix.Models.Screen> screenOptional = screenRepository.findById(showEntryDTO.getScreenId());
        if (screenOptional.isEmpty()) {
            throw new Exception("Screen not found with id: " + showEntryDTO.getScreenId());
        }

        com.example.TicketFlix.Models.Screen screen = screenOptional.get();
        if (screen.getTheater().getId() != showEntryDTO.getTheaterId()) {
            throw new Exception("Screen does not belong to the specified theater");
        }

        // Publish to Kafka (consumer will handle DB persistence)
        kafkaProducerService.publishShowCreationEvent(showEntryDTO);
        log.info("Show creation event published to Kafka for movie ID: {}, theater ID: {}, screen ID: {}", 
                showEntryDTO.getMovieId(), showEntryDTO.getTheaterId(), showEntryDTO.getScreenId());
        return "Show creation request submitted successfully";
    }

    public String updateShow(int showId, ShowEntryDTO showEntryDTO) throws Exception {
        Optional<Show> showOptional = showRepository.findById(showId);
        if (showOptional.isEmpty()) {
            throw new Exception("Show not found with id: " + showId);
        }

        // Publish to Kafka
        kafkaProducerService.publishShowUpdateEvent(showId, showEntryDTO);
        log.info("Show update event published to Kafka for show ID: {}", showId);
        return "Show update request submitted successfully";
    }

    public String deleteShow(int showId) throws Exception {
        Optional<Show> showOptional = showRepository.findById(showId);
        if (showOptional.isEmpty()) {
            throw new Exception("Show not found with id: " + showId);
        }

        // Publish to Kafka
        kafkaProducerService.publishShowDeletionEvent(showId);
        log.info("Show deletion event published to Kafka for show ID: {}", showId);
        return "Show deletion request submitted successfully";
    }

    public List<ShowEntryDTO> getAllShows() throws Exception {
        List<Show> shows = showRepository.findAll();
        // Optimized: Use Stream API for cleaner code
        return shows.stream()
                .map(ShowConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public ShowEntryDTO getShowById(int showId) throws Exception {
        // Check Redis cache first
        String cacheKey = SHOW_CACHE_KEY + showId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedData != null) {
            log.info("Show found in cache for ID: {}", showId);
        }

        Optional<Show> showOptional = showRepository.findById(showId);
        if (showOptional.isEmpty()) {
            throw new Exception("Show not found with id: " + showId);
        }

        Show show = showOptional.get();
        ShowEntryDTO showEntryDTO = ShowConvertor.convertEntityToDto(show);

        // Cache the show data
        try {
            String cacheValue = show.getMovie() != null ? show.getMovie().getMovieName() : "unknown";
            redisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache show data: {}", e.getMessage());
        }

        return showEntryDTO;
    }

    public List<ShowEntryDTO> getShowsByMovie(int movieId) throws Exception {
        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isEmpty()) {
            throw new Exception("Movie not found with id: " + movieId);
        }

        Movie movie = movieOptional.get();
        List<Show> shows = movie.getShowList();
        // Optimized: Use Stream API for cleaner code
        return shows.stream()
                .map(ShowConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ShowEntryDTO> getShowsByTheater(int theaterId) throws Exception {
        Optional<Theater> theaterOptional = theaterRepository.findById(theaterId);
        if (theaterOptional.isEmpty()) {
            throw new Exception("Theater not found with id: " + theaterId);
        }

        Theater theater = theaterOptional.get();
        List<Show> shows = theater.getShowList();
        // Optimized: Use Stream API for cleaner code
        return shows.stream()
                .map(ShowConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    // This method is called by Kafka consumer to actually create show
    @org.springframework.transaction.annotation.Transactional
    public String createShowInDB(ShowEntryDTO showEntryDTO) {
        // Create a showEntity
        Show show = ShowConvertor.convertDtoToEntity(showEntryDTO);

        int movieId = showEntryDTO.getMovieId();

        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isEmpty()) {
            throw new RuntimeException("Movie not found with id: " + movieId);
        }
        Movie movie = movieOptional.get();
        
        Optional<Theater> theaterOptional = theaterRepository.findById(showEntryDTO.getTheaterId());
        if (theaterOptional.isEmpty()) {
            throw new RuntimeException("Theater not found with id: " + showEntryDTO.getTheaterId());
        }
        Theater theater = theaterOptional.get();

        // Validate and get screen
        Optional<com.example.TicketFlix.Models.Screen> screenOptional = screenRepository.findById(showEntryDTO.getScreenId());
        if (screenOptional.isEmpty()) {
            throw new RuntimeException("Screen not found with id: " + showEntryDTO.getScreenId());
        }
        com.example.TicketFlix.Models.Screen screen = screenOptional.get();

        //Setting the attribute of foreign key
        show.setMovie(movie);
        show.setTheater(theater);
        show.setScreen(screen); // Set screen reference
        show.setShowDate(showEntryDTO.getLocalDate());
        show.setShowTime(showEntryDTO.getLocalTime());

        //Pending attributes the listOfShowSeatsEntity (use screen seats, not theater seats)
        List<ShowSeat> showSeatList = createShowSeatEntity(showEntryDTO, show, screen);
        show.setListOfShowSeats(showSeatList);

        show = showRepository.save(show);

        //Now we  also need to update the parent entities
        List<Show> showList = movie.getShowList();
        showList.add(show);
        movie.setShowList(showList);
        movieRepository.save(movie);

        List<Show> showList1 = theater.getShowList();
        showList1.add(show);
        theater.setShowList(showList1);
        theaterRepository.save(theater);

        // Update screen's show list
        List<Show> screenShowList = screen.getShowList();
        screenShowList.add(show);
        screen.setShowList(screenShowList);
        screenRepository.save(screen);

        return "The show has been added successfully";
    }

    private List<ShowSeat> createShowSeatEntity(ShowEntryDTO showEntryDTO, Show show, com.example.TicketFlix.Models.Screen screen){
        //Now the goal is to create the ShowSeatEntity
        //We need to set its attribute from Screen's seats (not Theater's seats)

        List<TheaterSeats> theaterSeats = screen.getTheaterSeatsList();

        List<ShowSeat> seatList = new ArrayList<>();

        for(TheaterSeats theaterSeats1 : theaterSeats){

            ShowSeat showSeat = new ShowSeat();

            showSeat.setSeatNumber(theaterSeats1.getSeatNumber());
            showSeat.setSeatType(theaterSeats1.getSeatType());

            // Optimized: Use == for enum comparison instead of .equals()
            if(theaterSeats1.getSeatType() == SeatType.CLASSIC)
                showSeat.setPrice(showEntryDTO.getClassSeatPrice());
            else
                showSeat.setPrice(showEntryDTO.getPremiumSeatPrice());

            showSeat.setBooked(false);
            showSeat.setShow(show); //parent : foreign key for the showSeat Entity

            seatList.add(showSeat); //Adding it to the list
        }
        return seatList;
    }
}
