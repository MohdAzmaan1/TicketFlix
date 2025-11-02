package com.example.TicketFlix.Models;

import com.example.TicketFlix.Genres.ShowType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDate showDate;

    private LocalTime showTime;

    @CreationTimestamp
    private Date createdOn;

    @UpdateTimestamp
    private Date updatedOn;

    @Enumerated(value = EnumType.STRING)
    private ShowType showType;

    //Child wrt Theater (for backward compatibility and queries)
    @ManyToOne
    @JoinColumn
    private Theater theater;

    //Show belongs to a Screen
    @ManyToOne
    @JoinColumn
    private Screen screen;

    //Child wrt to Movie
    @ManyToOne
    @JoinColumn
    private Movie movie;

    //Show is parent wrt tickets
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    private List<Ticket>listOfBookedTickets  = new ArrayList<>();

    ////Show is parent wrt show seats
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    private List<ShowSeat>listOfShowSeats  = new ArrayList<>();
}
