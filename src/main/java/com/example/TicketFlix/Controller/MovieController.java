package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.MovieEntryDTO;
import com.example.TicketFlix.Response.ApiResponse;
import com.example.TicketFlix.Response.ResponseFactory;
import com.example.TicketFlix.Service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    MovieService movieService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addMovie(@RequestBody MovieEntryDTO movieEntryDTO, HttpServletRequest request){
        try{
            String msg = movieService.addMovie(movieEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(msg, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<MovieEntryDTO>>> getAllMovies(HttpServletRequest request){
        try{
            List<MovieEntryDTO> movies = movieService.getAllMovies();
            return new ResponseEntity<>(ResponseFactory.success(movies, "Movies fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<MovieEntryDTO>> body = ApiResponse.<List<MovieEntryDTO>>builder()
                    .success(false)
                    .message("Failed to fetch movies")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{movieId}")
    public ResponseEntity<ApiResponse<MovieEntryDTO>> getMovieById(@PathVariable int movieId, HttpServletRequest request){
        try{
            MovieEntryDTO movie = movieService.getMovieById(movieId);
            return new ResponseEntity<>(ResponseFactory.success(movie, "Movie fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<MovieEntryDTO> body = ApiResponse.<MovieEntryDTO>builder()
                    .success(false)
                    .message("Movie not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{movieId}")
    public ResponseEntity<ApiResponse<Void>> updateMovie(@PathVariable int movieId, @RequestBody MovieEntryDTO movieEntryDTO, HttpServletRequest request){
        try{
            String msg = movieService.updateMovie(movieId, movieEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(msg, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{movieId}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable int movieId, HttpServletRequest request){
        try{
            String msg = movieService.deleteMovie(movieId);
            ApiResponse<Void> body = ResponseFactory.ack(msg, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<String>>> getTrendingMovies(@RequestParam(defaultValue = "10") int limit, HttpServletRequest request){
        try {
            Set<String> trendingMovies = movieService.getTrendingMovies(limit);
            return new ResponseEntity<>(ResponseFactory.success(new ArrayList<>(trendingMovies), "Trending movies fetched", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<String>> body = ApiResponse.<List<String>>builder()
                    .success(false)
                    .message("Failed to fetch trending movies")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/trending-with-counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTrendingMoviesWithCounts(@RequestParam(defaultValue = "10") int limit, HttpServletRequest request) {
        try {
            Map<String, Long> trendingMovies = movieService.getTrendingMoviesWithCounts(limit);
            return new ResponseEntity<>(ResponseFactory.success(trendingMovies, "Trending movies with counts fetched", request), HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<Map<String, Long>> body = ApiResponse.<Map<String, Long>>builder()
                    .success(false)
                    .message("Failed to fetch trending movies with counts")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
