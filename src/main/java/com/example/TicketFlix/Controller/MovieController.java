package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.MovieEntryDTO;
import com.example.TicketFlix.Service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    MovieService movieService;

    @PostMapping("/add")
    public ResponseEntity<String> addMovie(@RequestBody MovieEntryDTO movieEntryDTO){
        try{
            String response = movieService.addMovie(movieEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<MovieEntryDTO>> getAllMovies(){
        try{
            List<MovieEntryDTO> movies = movieService.getAllMovies();
            return new ResponseEntity<>(movies, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{movieId}")
    public ResponseEntity<MovieEntryDTO> getMovieById(@PathVariable int movieId){
        try{
            MovieEntryDTO movie = movieService.getMovieById(movieId);
            return new ResponseEntity<>(movie, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{movieId}")
    public ResponseEntity<String> updateMovie(@PathVariable int movieId, @RequestBody MovieEntryDTO movieEntryDTO){
        try{
            String response = movieService.updateMovie(movieId, movieEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{movieId}")
    public ResponseEntity<String> deleteMovie(@PathVariable int movieId){
        try{
            String response = movieService.deleteMovie(movieId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrendingMovies(@RequestParam(defaultValue = "10") int limit){
        try {
            Set<String> trendingMovies = movieService.getTrendingMovies(limit);
            return new ResponseEntity<>(new ArrayList<>(trendingMovies), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/trending-with-counts")
    public ResponseEntity<Map<String, Long>> getTrendingMoviesWithCounts(@RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Long> trendingMovies = movieService.getTrendingMoviesWithCounts(limit);
            return new ResponseEntity<>(trendingMovies, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
