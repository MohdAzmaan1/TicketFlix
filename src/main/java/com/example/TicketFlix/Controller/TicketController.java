package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.DeleteTicketEntryDTO;
import com.example.TicketFlix.EntryDTOs.TicketEntryDTO;
import com.example.TicketFlix.Response.ApiResponse;
import com.example.TicketFlix.Response.TicketResponseDTO;
import com.example.TicketFlix.Response.ResponseFactory;
import com.example.TicketFlix.Service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    TicketService ticketService;

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Void>> addTicket(@RequestBody TicketEntryDTO ticketEntryDTO, HttpServletRequest request){
        try{
            String response = ticketService.addTicket(ticketEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/cancel-ticket")
    public ResponseEntity<ApiResponse<Void>> cancelTicket(@RequestBody DeleteTicketEntryDTO deleteTicketEntryDto, HttpServletRequest request) {
        try{
            String response = ticketService.cancelTicket(deleteTicketEntryDto);
            ApiResponse<Void> body = ResponseFactory.ack(response, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure(e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getAllTickets(HttpServletRequest request){
        try{
            List<TicketResponseDTO> tickets = ticketService.getAllTickets();
            return new ResponseEntity<>(ResponseFactory.success(tickets, "Tickets fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<TicketResponseDTO>> body = ApiResponse.<List<TicketResponseDTO>>builder()
                    .success(false)
                    .message("Failed to fetch tickets")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{ticketId}")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> getTicketById(@PathVariable int ticketId, HttpServletRequest request){
        try{
            TicketResponseDTO ticket = ticketService.getTicketById(ticketId);
            return new ResponseEntity<>(ResponseFactory.success(ticket, "Ticket fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<TicketResponseDTO> body = ApiResponse.<TicketResponseDTO>builder()
                    .success(false)
                    .message("Ticket not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-ticket-id/{ticketId}")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> getTicketByTicketId(@PathVariable String ticketId, HttpServletRequest request){
        try{
            TicketResponseDTO ticket = ticketService.getTicketById(Integer.parseInt(ticketId));
            return new ResponseEntity<>(ResponseFactory.success(ticket, "Ticket fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<TicketResponseDTO> body = ApiResponse.<TicketResponseDTO>builder()
                    .success(false)
                    .message("Ticket not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-user/{userId}")
    public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getTicketsByUser(@PathVariable int userId, HttpServletRequest request){
        try{
            List<TicketResponseDTO> tickets = ticketService.getTicketsByUser(userId);
            return new ResponseEntity<>(ResponseFactory.success(tickets, "Tickets fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<TicketResponseDTO>> body = ApiResponse.<List<TicketResponseDTO>>builder()
                    .success(false)
                    .message("Tickets not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-by-show/{showId}")
    public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getTicketsByShow(@PathVariable int showId, HttpServletRequest request){
        try{
            List<TicketResponseDTO> tickets = ticketService.getTicketsByShow(showId);
            return new ResponseEntity<>(ResponseFactory.success(tickets, "Tickets fetched successfully", request), HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<TicketResponseDTO>> body = ApiResponse.<List<TicketResponseDTO>>builder()
                    .success(false)
                    .message("Tickets not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }
}
