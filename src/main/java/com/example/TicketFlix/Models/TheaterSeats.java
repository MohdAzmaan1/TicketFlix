package com.example.TicketFlix.Models;

import com.example.TicketFlix.Genres.SeatType;
import lombok.*;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TheaterSeats")
public class TheaterSeats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String seatNumber;

    @Enumerated(value = EnumType.STRING)
    private SeatType seatType;

    // Seat belongs to a Screen (not directly to Theater)
    @ManyToOne
    @JoinColumn
    private Screen screen;
}
