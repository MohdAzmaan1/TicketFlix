package com.example.TicketFlix.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScreenResponseDTO {
    private int id;
    private String name;
    private int screenNumber;
    private int theaterId;
    private String theaterName;
    private int classicSeatsCount;
    private int premiumSeatsCount;
}

