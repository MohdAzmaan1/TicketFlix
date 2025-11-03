package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.TheaterEntryDTO;
import com.example.TicketFlix.Response.ApiResponse;
import com.example.TicketFlix.Response.ResponseFactory;
import com.example.TicketFlix.Service.TheaterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/theater")
public class TheaterController {

    @Autowired
    TheaterService theaterService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addTheater(@RequestBody TheaterEntryDTO theaterEntryDTO, HttpServletRequest request){
        try{
            String response = theaterService.addTheater(theaterEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch(Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<TheaterEntryDTO>>> getAllTheaters(HttpServletRequest request){
        try{
            List<TheaterEntryDTO> theaters = theaterService.getAllTheaters();
            return new ResponseEntity<>(ResponseFactory.success(theaters, "Theaters fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<TheaterEntryDTO>> body = ApiResponse.<List<TheaterEntryDTO>>builder()
                    .success(false)
                    .message("Failed to fetch theaters")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{theaterId}")
    public ResponseEntity<ApiResponse<TheaterEntryDTO>> getTheaterById(@PathVariable int theaterId, HttpServletRequest request){
        try{
            TheaterEntryDTO theater = theaterService.getTheaterById(theaterId);
            return new ResponseEntity<>(ResponseFactory.success(theater, "Theater fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<TheaterEntryDTO> body = ApiResponse.<TheaterEntryDTO>builder()
                    .success(false)
                    .message("Theater not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{theaterId}")
    public ResponseEntity<ApiResponse<Void>> updateTheater(@PathVariable int theaterId, @RequestBody TheaterEntryDTO theaterEntryDTO, HttpServletRequest request){
        try{
            String response = theaterService.updateTheater(theaterId, theaterEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{theaterId}")
    public ResponseEntity<ApiResponse<Void>> deleteTheater(@PathVariable int theaterId, HttpServletRequest request){
        try{
            String response = theaterService.deleteTheater(theaterId);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }
}
