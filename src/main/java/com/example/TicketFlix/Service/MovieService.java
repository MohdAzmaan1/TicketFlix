package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.MovieConvertor;
import com.example.TicketFlix.EntryDTOs.MovieEntryDTO;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.Movie;
import com.example.TicketFlix.Repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MovieService {

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    RedisService redisService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    KafkaProducerService kafkaProducerService;

    private static final String MOVIE_CACHE_KEY = "movie::";
    private static final int CACHE_TTL_HOURS = 24;

    public String addMovie(MovieEntryDTO movieEntryDTO) throws Exception {
        // Check if movie with same name exists
        Optional<Movie> existingMovie = movieRepository.findByMovieName(movieEntryDTO.getMovieName());
        if (existingMovie.isPresent()) {
            throw new Exception("Movie with name " + movieEntryDTO.getMovieName() + " already exists");
        }

        // Publish to Kafka (consumer will handle DB persistence)
        kafkaProducerService.publishMovieCreationEvent(movieEntryDTO);
        log.info("Movie creation event published to Kafka for: {}", movieEntryDTO.getMovieName());
        return "Movie creation request submitted successfully";
    }

    public String updateMovie(int movieId, MovieEntryDTO movieEntryDTO) throws Exception {
        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isEmpty()) {
            throw new Exception("Movie not found with id: " + movieId);
        }

        // Publish to Kafka
        kafkaProducerService.publishMovieUpdateEvent(movieId, movieEntryDTO);
        log.info("Movie update event published to Kafka for movie ID: {}", movieId);
        return "Movie update request submitted successfully";
    }

    public String deleteMovie(int movieId) throws Exception {
        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isEmpty()) {
            throw new Exception("Movie not found with id: " + movieId);
        }

        // Publish to Kafka
        kafkaProducerService.publishMovieDeletionEvent(movieId);
        log.info("Movie deletion event published to Kafka for movie ID: {}", movieId);
        return "Movie deletion request submitted successfully";
    }

    public List<MovieEntryDTO> getAllMovies() throws Exception {
        List<Movie> movies = movieRepository.findAll();
        // Optimized: Use Stream API for cleaner code
        return movies.stream()
                .map(MovieConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public MovieEntryDTO getMovieById(int movieId) throws Exception {
        // Check Redis cache first
        String cacheKey = MOVIE_CACHE_KEY + movieId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedData != null) {
            log.info("Movie found in cache for ID: {}", movieId);
            // Parse from cache if needed (for now, fetch from DB)
        }

        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isEmpty()) {
            throw new Exception("Movie not found with id: " + movieId);
        }

        Movie movie = movieOptional.get();
        MovieEntryDTO movieEntryDTO = MovieConvertor.convertEntityToDto(movie);

        // Cache the movie data
        try {
            redisTemplate.opsForValue().set(cacheKey, movie.getMovieName(), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache movie data: {}", e.getMessage());
        }

        return movieEntryDTO;
    }

    public Set<String> getTrendingMovies(int limit) {
        return redisService.getTrendingMovies(limit);
    }

    public Map<String, Long> getTrendingMoviesWithCounts(int limit) {
        Set<ZSetOperations.TypedTuple<String>> trendingWithScores = redisService.getTrendingMoviesWithCounts(limit);
        Map<String, Long> result = new LinkedHashMap<>();

        for (ZSetOperations.TypedTuple<String> tuple : trendingWithScores) {
            result.put(tuple.getValue(), Objects.requireNonNull(tuple.getScore()).longValue());
        }

        return result;
    }
}
