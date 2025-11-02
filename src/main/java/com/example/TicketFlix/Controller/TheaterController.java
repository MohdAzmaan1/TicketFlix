package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.TheaterEntryDTO;
import com.example.TicketFlix.Service.TheaterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/theater")
public class TheaterController {

    @Autowired
    TheaterService theaterService;

    @PostMapping("/add")
    public ResponseEntity<String> addTheater(@RequestBody TheaterEntryDTO theaterEntryDTO){
        try{
            String response = theaterService.addTheater(theaterEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch(Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<TheaterEntryDTO>> getAllTheaters(){
        try{
            List<TheaterEntryDTO> theaters = theaterService.getAllTheaters();
            return new ResponseEntity<>(theaters, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{theaterId}")
    public ResponseEntity<TheaterEntryDTO> getTheaterById(@PathVariable int theaterId){
        try{
            TheaterEntryDTO theater = theaterService.getTheaterById(theaterId);
            return new ResponseEntity<>(theater, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{theaterId}")
    public ResponseEntity<String> updateTheater(@PathVariable int theaterId, @RequestBody TheaterEntryDTO theaterEntryDTO){
        try{
            String response = theaterService.updateTheater(theaterId, theaterEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{theaterId}")
    public ResponseEntity<String> deleteTheater(@PathVariable int theaterId){
        try{
            String response = theaterService.deleteTheater(theaterId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
