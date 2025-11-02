package com.example.TicketFlix.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RedisService {
    private static final String MOVIE_BOOKING_KEY = "movie-";
    private static final String TRENDING_MOVIE_KEY = "trending::movie::count";

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    public void increaseMovieCounter(String movieName) {
        String key = MOVIE_BOOKING_KEY + movieName;
        Long current = redisTemplate.opsForValue().increment(key);
        redisTemplate.opsForZSet().add(TRENDING_MOVIE_KEY, movieName, current);
    }

    public void decreaseCounter(String movieName) {
        String key = MOVIE_BOOKING_KEY + movieName;
        Long current = redisTemplate.opsForValue().decrement(key);

        if(current > 0){
            redisTemplate.opsForZSet().add(TRENDING_MOVIE_KEY,movieName,current);
        } else {
            redisTemplate.opsForZSet().remove(TRENDING_MOVIE_KEY, movieName);
        }
    }
    public Set<String> getTrendingMovies(int limit) {
        return redisTemplate.opsForZSet().reverseRange(TRENDING_MOVIE_KEY, 0, limit - 1);
    }

    /**
     * Get trending movies with their booking counts
     */
    public Set<ZSetOperations.TypedTuple<String>> getTrendingMoviesWithCounts(int limit) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(TRENDING_MOVIE_KEY, 0, limit - 1);
    }

    /**
     * Get booking count for a specific movie
     */
    public Long getMovieBookingCount(String movieName) {
        String key = MOVIE_BOOKING_KEY + movieName;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }
}
