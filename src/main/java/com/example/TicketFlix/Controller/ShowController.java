package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.ShowEntryDTO;
import com.example.TicketFlix.Service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shows")
public class ShowController {

    @Autowired
    ShowService showService;

    @PostMapping("/add")
    public ResponseEntity<String> addShow(@RequestBody ShowEntryDTO showEntryDTO){
        try{
            String response = showService.addShow(showEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<ShowEntryDTO>> getAllShows(){
        try{
            List<ShowEntryDTO> shows = showService.getAllShows();
            return new ResponseEntity<>(shows, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{showId}")
    public ResponseEntity<ShowEntryDTO> getShowById(@PathVariable int showId){
        try{
            ShowEntryDTO show = showService.getShowById(showId);
            return new ResponseEntity<>(show, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-movie/{movieId}")
    public ResponseEntity<List<ShowEntryDTO>> getShowsByMovie(@PathVariable int movieId){
        try{
            List<ShowEntryDTO> shows = showService.getShowsByMovie(movieId);
            return new ResponseEntity<>(shows, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-theater/{theaterId}")
    public ResponseEntity<List<ShowEntryDTO>> getShowsByTheater(@PathVariable int theaterId){
        try{
            List<ShowEntryDTO> shows = showService.getShowsByTheater(theaterId);
            return new ResponseEntity<>(shows, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{showId}")
    public ResponseEntity<String> updateShow(@PathVariable int showId, @RequestBody ShowEntryDTO showEntryDTO){
        try{
            String response = showService.updateShow(showId, showEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{showId}")
    public ResponseEntity<String> deleteShow(@PathVariable int showId){
        try{
            String response = showService.deleteShow(showId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
