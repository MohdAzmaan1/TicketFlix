package com.example.TicketFlix.Convertors;

import com.example.TicketFlix.EntryDTOs.ScreenEntryDTO;
import com.example.TicketFlix.Models.Screen;
import com.example.TicketFlix.Response.ScreenResponseDTO;

public class ScreenConvertor {

    public static Screen convertDtoToEntity(ScreenEntryDTO screenEntryDTO) {
        return Screen.builder()
                .name(screenEntryDTO.getName())
                .screenNumber(screenEntryDTO.getScreenNumber())
                .build();
    }

    public static ScreenResponseDTO convertEntityToDto(Screen screen) {
        ScreenResponseDTO screenResponseDTO = ScreenResponseDTO.builder()
                .id(screen.getId())
                .name(screen.getName())
                .screenNumber(screen.getScreenNumber())
                .build();

        if (screen.getTheater() != null) {
            screenResponseDTO.setTheaterId(screen.getTheater().getId());
            screenResponseDTO.setTheaterName(screen.getTheater().getName());
        }

        // Calculate seat counts from theaterSeatsList
        if (screen.getTheaterSeatsList() != null) {
            long classicCount = screen.getTheaterSeatsList().stream()
                    .filter(seat -> seat.getSeatType() == com.example.TicketFlix.Genres.SeatType.CLASSIC)
                    .count();
            long premiumCount = screen.getTheaterSeatsList().stream()
                    .filter(seat -> seat.getSeatType() == com.example.TicketFlix.Genres.SeatType.PREMIUM)
                    .count();
            screenResponseDTO.setClassicSeatsCount((int) classicCount);
            screenResponseDTO.setPremiumSeatsCount((int) premiumCount);
        }

        return screenResponseDTO;
    }
}

