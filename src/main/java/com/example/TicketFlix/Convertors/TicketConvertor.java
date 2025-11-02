package com.example.TicketFlix.Convertors;

import com.example.TicketFlix.Models.Ticket;
import com.example.TicketFlix.Response.TicketResponseDTO;

public class TicketConvertor {

    public static TicketResponseDTO convertEntityToDto(Ticket ticket) {
        TicketResponseDTO ticketResponseDTO = new TicketResponseDTO();
        ticketResponseDTO.setId(ticket.getId());
        ticketResponseDTO.setTicketId(ticket.getTicketId());
        ticketResponseDTO.setMovieName(ticket.getMovieName());
        ticketResponseDTO.setTheaterName(ticket.getTheaterName());
        ticketResponseDTO.setShowTime(ticket.getShowTime());
        ticketResponseDTO.setShowDate(ticket.getShowDate());
        ticketResponseDTO.setBookedSeat(ticket.getBookedSeat());
        ticketResponseDTO.setTotalAmount(ticket.getTotalAmount());
        
        if (ticket.getUser() != null) {
            ticketResponseDTO.setUserId(ticket.getUser().getId());
            ticketResponseDTO.setUserName(ticket.getUser().getName());
            ticketResponseDTO.setUserEmail(ticket.getUser().getEmail());
        }
        
        if (ticket.getShow() != null) {
            ticketResponseDTO.setShowId(ticket.getShow().getId());
        }
        
        return ticketResponseDTO;
    }
}

