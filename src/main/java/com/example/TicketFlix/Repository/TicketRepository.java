package com.example.TicketFlix.Repository;

import com.example.TicketFlix.Models.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket,Integer> {
    @Query("SELECT t FROM Ticket t WHERE t.user.id = :userId")
    List<Ticket> findByUserId(@Param("userId") int userId);
    
    @Query("SELECT t FROM Ticket t WHERE t.show.id = :showId")
    List<Ticket> findByShowId(@Param("showId") int showId);
    
    Optional<Ticket> findByTicketId(String ticketId);
}
