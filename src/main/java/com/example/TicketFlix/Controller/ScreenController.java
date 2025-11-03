package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.ScreenEntryDTO;
import com.example.TicketFlix.Response.ApiResponse;
import com.example.TicketFlix.Response.ScreenResponseDTO;
import com.example.TicketFlix.Response.ResponseFactory;
import com.example.TicketFlix.Service.ScreenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/screens")
public class ScreenController {

    @Autowired
    ScreenService screenService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addScreen(@RequestBody ScreenEntryDTO screenEntryDTO, HttpServletRequest request) {
        try {
            String response = screenService.addScreen(screenEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<ScreenResponseDTO>>> getAllScreens(HttpServletRequest request) {
        try {
            List<ScreenResponseDTO> screens = screenService.getAllScreens();
            return new ResponseEntity<>(ResponseFactory.success(screens, "Screens fetched successfully", request), HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<List<ScreenResponseDTO>> body = ApiResponse.<List<ScreenResponseDTO>>builder()
                    .success(false)
                    .message("Failed to fetch screens")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{screenId}")
    public ResponseEntity<ApiResponse<ScreenResponseDTO>> getScreenById(@PathVariable int screenId, HttpServletRequest request) {
        try {
            ScreenResponseDTO screen = screenService.getScreenById(screenId);
            return new ResponseEntity<>(ResponseFactory.success(screen, "Screen fetched successfully", request), HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<ScreenResponseDTO> body = ApiResponse.<ScreenResponseDTO>builder()
                    .success(false)
                    .message("Screen not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-theater/{theaterId}")
    public ResponseEntity<ApiResponse<List<ScreenResponseDTO>>> getScreensByTheater(@PathVariable int theaterId, HttpServletRequest request) {
        try {
            List<ScreenResponseDTO> screens = screenService.getScreensByTheater(theaterId);
            return new ResponseEntity<>(ResponseFactory.success(screens, "Screens fetched successfully", request), HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<List<ScreenResponseDTO>> body = ApiResponse.<List<ScreenResponseDTO>>builder()
                    .success(false)
                    .message("Screens not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{screenId}")
    public ResponseEntity<ApiResponse<Void>> updateScreen(@PathVariable int screenId, @RequestBody ScreenEntryDTO screenEntryDTO, HttpServletRequest request) {
        try {
            String response = screenService.updateScreen(screenId, screenEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{screenId}")
    public ResponseEntity<ApiResponse<Void>> deleteScreen(@PathVariable int screenId, HttpServletRequest request) {
        try {
            String response = screenService.deleteScreen(screenId);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }
}

