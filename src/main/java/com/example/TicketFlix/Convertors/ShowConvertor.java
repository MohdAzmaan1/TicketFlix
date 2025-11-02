package com.example.TicketFlix.Convertors;

import com.example.TicketFlix.EntryDTOs.ShowEntryDTO;
import com.example.TicketFlix.Models.Show;
import com.example.TicketFlix.Models.ShowSeat;
import com.example.TicketFlix.Genres.SeatType;

public class ShowConvertor {
    public static Show convertDtoToEntity(ShowEntryDTO showEntryDTO) {
        return Show.builder()
                .showDate(showEntryDTO.getLocalDate())
                .showTime(showEntryDTO.getLocalTime())
                .showType(showEntryDTO.getShowType())
                .build();
    }

    public static ShowEntryDTO convertEntityToDto(Show show) {
        ShowEntryDTO showEntryDTO = new ShowEntryDTO();
        showEntryDTO.setLocalDate(show.getShowDate());
        showEntryDTO.setLocalTime(show.getShowTime());
        showEntryDTO.setShowType(show.getShowType());
        
        if (show.getMovie() != null) {
            showEntryDTO.setMovieId(show.getMovie().getId());
        }
        if (show.getTheater() != null) {
            showEntryDTO.setTheaterId(show.getTheater().getId());
        }
        
        if (show.getScreen() != null) {
            showEntryDTO.setScreenId(show.getScreen().getId());
        }
        
        // Extract prices from show seats if available
        if (show.getListOfShowSeats() != null && !show.getListOfShowSeats().isEmpty()) {
            // Optimized: Use enum comparison instead of string comparison
            int classPrice = show.getListOfShowSeats().stream()
                    .filter(seat -> seat.getSeatType() == SeatType.CLASSIC)
                    .findFirst()
                    .map(ShowSeat::getPrice)
                    .orElse(0);
            int premiumPrice = show.getListOfShowSeats().stream()
                    .filter(seat -> seat.getSeatType() == SeatType.PREMIUM)
                    .findFirst()
                    .map(ShowSeat::getPrice)
                    .orElse(0);
            showEntryDTO.setClassSeatPrice(classPrice);
            showEntryDTO.setPremiumSeatPrice(premiumPrice);
        }
        
        return showEntryDTO;
    }
}
