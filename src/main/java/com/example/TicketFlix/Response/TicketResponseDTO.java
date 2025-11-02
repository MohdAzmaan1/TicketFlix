package com.example.TicketFlix.Response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TicketResponseDTO {
    private int id;
    private String ticketId;
    private String movieName;
    private String theaterName;
    private LocalTime showTime;
    private LocalDate showDate;
    private String bookedSeat;
    private int totalAmount;
    private int userId;
    private String userName;
    private String userEmail;
    private int showId;
}

