package com.example.TicketFlix.EntryDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TheaterEntryDTO {

    private String name;

    private String location;

    // Deprecated: Use screens instead (each screen has its own seats)
    @Deprecated
    private int classicSeatsCount;

    @Deprecated
    private int premiumSeatsCount;
    
    // List of screens to create (optional - screens can be added separately)
    @lombok.Builder.Default
    private List<ScreenEntryDTO> screens = new ArrayList<>();
}
