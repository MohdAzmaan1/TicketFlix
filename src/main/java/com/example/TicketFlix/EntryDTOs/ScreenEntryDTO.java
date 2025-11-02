package com.example.TicketFlix.EntryDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScreenEntryDTO {
    private String name; // e.g., "Screen 1", "IMAX", "Audi 1"
    private int screenNumber;
    private int theaterId;
    private int classicSeatsCount;
    private int premiumSeatsCount;
}

