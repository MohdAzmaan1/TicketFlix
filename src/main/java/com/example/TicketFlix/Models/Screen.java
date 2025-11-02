package com.example.TicketFlix.Models;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screens")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private int screenNumber;

    @ManyToOne
    @JoinColumn
    private Theater theater;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TheaterSeats> theaterSeatsList = new ArrayList<>();

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Show> showList = new ArrayList<>();
}

