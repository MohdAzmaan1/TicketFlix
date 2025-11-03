package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.ShowEntryDTO;
import com.example.TicketFlix.Response.ApiResponse;
import com.example.TicketFlix.Response.ResponseFactory;
import com.example.TicketFlix.Service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/shows")
public class ShowController {

    @Autowired
    ShowService showService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addShow(@RequestBody ShowEntryDTO showEntryDTO, HttpServletRequest request){
        try{
            String response = showService.addShow(showEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<ShowEntryDTO>>> getAllShows(HttpServletRequest request){
        try{
            List<ShowEntryDTO> shows = showService.getAllShows();
            return new ResponseEntity<>(ResponseFactory.success(shows, "Shows fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<ShowEntryDTO>> body = ApiResponse.<List<ShowEntryDTO>>builder()
                    .success(false)
                    .message("Failed to fetch shows")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{showId}")
    public ResponseEntity<ApiResponse<ShowEntryDTO>> getShowById(@PathVariable int showId, HttpServletRequest request){
        try{
            ShowEntryDTO show = showService.getShowById(showId);
            return new ResponseEntity<>(ResponseFactory.success(show, "Show fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<ShowEntryDTO> body = ApiResponse.<ShowEntryDTO>builder()
                    .success(false)
                    .message("Show not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-movie/{movieId}")
    public ResponseEntity<ApiResponse<List<ShowEntryDTO>>> getShowsByMovie(@PathVariable int movieId, HttpServletRequest request){
        try{
            List<ShowEntryDTO> shows = showService.getShowsByMovie(movieId);
            return new ResponseEntity<>(ResponseFactory.success(shows, "Shows fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<ShowEntryDTO>> body = ApiResponse.<List<ShowEntryDTO>>builder()
                    .success(false)
                    .message("Shows not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-theater/{theaterId}")
    public ResponseEntity<ApiResponse<List<ShowEntryDTO>>> getShowsByTheater(@PathVariable int theaterId, HttpServletRequest request){
        try{
            List<ShowEntryDTO> shows = showService.getShowsByTheater(theaterId);
            return new ResponseEntity<>(ResponseFactory.success(shows, "Shows fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<ShowEntryDTO>> body = ApiResponse.<List<ShowEntryDTO>>builder()
                    .success(false)
                    .message("Shows not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{showId}")
    public ResponseEntity<ApiResponse<Void>> updateShow(@PathVariable int showId, @RequestBody ShowEntryDTO showEntryDTO, HttpServletRequest request){
        try{
            String response = showService.updateShow(showId, showEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{showId}")
    public ResponseEntity<ApiResponse<Void>> deleteShow(@PathVariable int showId, HttpServletRequest request){
        try{
            String response = showService.deleteShow(showId);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }
}
