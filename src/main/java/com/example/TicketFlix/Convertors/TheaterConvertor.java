package com.example.TicketFlix.Convertors;

import com.example.TicketFlix.EntryDTOs.TheaterEntryDTO;
import com.example.TicketFlix.Models.Theater;

public class TheaterConvertor {

    public static Theater convertDtoToEntity(TheaterEntryDTO theaterEntryDTO){
        return Theater.builder().location(theaterEntryDTO.getLocation()).name(theaterEntryDTO.getName()).build();
    }

    public static TheaterEntryDTO convertEntityToDto(Theater theater){
        TheaterEntryDTO theaterEntryDTO = new TheaterEntryDTO();
        theaterEntryDTO.setName(theater.getName());
        theaterEntryDTO.setLocation(theater.getLocation());
        
        // Note: Seats are now managed through Screens, not directly through Theater
        // Calculate total seat counts from all screens
        if (theater.getScreenList() != null && !theater.getScreenList().isEmpty()) {
            long totalClassicCount = 0;
            long totalPremiumCount = 0;
            
            for (com.example.TicketFlix.Models.Screen screen : theater.getScreenList()) {
                if (screen.getTheaterSeatsList() != null) {
                    long classicCount = screen.getTheaterSeatsList().stream()
                            .filter(seat -> seat.getSeatType() == com.example.TicketFlix.Genres.SeatType.CLASSIC)
                            .count();
                    long premiumCount = screen.getTheaterSeatsList().stream()
                            .filter(seat -> seat.getSeatType() == com.example.TicketFlix.Genres.SeatType.PREMIUM)
                            .count();
                    totalClassicCount += classicCount;
                    totalPremiumCount += premiumCount;
                }
            }
            
            // Set deprecated fields for backward compatibility
            theaterEntryDTO.setClassicSeatsCount((int) totalClassicCount);
            theaterEntryDTO.setPremiumSeatsCount((int) totalPremiumCount);
        }
        return theaterEntryDTO;
    }
}