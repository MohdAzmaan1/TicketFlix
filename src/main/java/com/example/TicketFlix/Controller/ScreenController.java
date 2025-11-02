package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.ScreenEntryDTO;
import com.example.TicketFlix.Response.ScreenResponseDTO;
import com.example.TicketFlix.Service.ScreenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/screens")
public class ScreenController {

    @Autowired
    ScreenService screenService;

    @PostMapping("/add")
    public ResponseEntity<String> addScreen(@RequestBody ScreenEntryDTO screenEntryDTO) {
        try {
            String response = screenService.addScreen(screenEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<ScreenResponseDTO>> getAllScreens() {
        try {
            List<ScreenResponseDTO> screens = screenService.getAllScreens();
            return new ResponseEntity<>(screens, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{screenId}")
    public ResponseEntity<ScreenResponseDTO> getScreenById(@PathVariable int screenId) {
        try {
            ScreenResponseDTO screen = screenService.getScreenById(screenId);
            return new ResponseEntity<>(screen, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-theater/{theaterId}")
    public ResponseEntity<List<ScreenResponseDTO>> getScreensByTheater(@PathVariable int theaterId) {
        try {
            List<ScreenResponseDTO> screens = screenService.getScreensByTheater(theaterId);
            return new ResponseEntity<>(screens, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{screenId}")
    public ResponseEntity<String> updateScreen(@PathVariable int screenId, @RequestBody ScreenEntryDTO screenEntryDTO) {
        try {
            String response = screenService.updateScreen(screenId, screenEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{screenId}")
    public ResponseEntity<String> deleteScreen(@PathVariable int screenId) {
        try {
            String response = screenService.deleteScreen(screenId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}

