package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.DeleteTicketEntryDTO;
import com.example.TicketFlix.EntryDTOs.TicketEntryDTO;
import com.example.TicketFlix.Response.TicketResponseDTO;
import com.example.TicketFlix.Service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    TicketService ticketService;

    @PostMapping("/book")
    public ResponseEntity<String> addTicket(@RequestBody TicketEntryDTO ticketEntryDTO){
        try{
            String response = ticketService.addTicket(ticketEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/cancel-ticket")
    public ResponseEntity<String> cancelTicket(@RequestBody DeleteTicketEntryDTO deleteTicketEntryDto) {
        try{
            String response = ticketService.cancelTicket(deleteTicketEntryDto);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(){
        try{
            List<TicketResponseDTO> tickets = ticketService.getAllTickets();
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{ticketId}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable int ticketId){
        try{
            TicketResponseDTO ticket = ticketService.getTicketById(ticketId);
            return new ResponseEntity<>(ticket, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-ticket-id/{ticketId}")
    public ResponseEntity<TicketResponseDTO> getTicketByTicketId(@PathVariable String ticketId){
        try{
            TicketResponseDTO ticket = ticketService.getTicketByTicketId(ticketId);
            return new ResponseEntity<>(ticket, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-user/{userId}")
    public ResponseEntity<List<TicketResponseDTO>> getTicketsByUser(@PathVariable int userId){
        try{
            List<TicketResponseDTO> tickets = ticketService.getTicketsByUser(userId);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-show/{showId}")
    public ResponseEntity<List<TicketResponseDTO>> getTicketsByShow(@PathVariable int showId){
        try{
            List<TicketResponseDTO> tickets = ticketService.getTicketsByShow(showId);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
