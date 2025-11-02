package com.example.TicketFlix.Repository;

import com.example.TicketFlix.Models.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Integer> {
    List<Screen> findByTheaterId(int theaterId);
    
    @Query(value = "SELECT s FROM Screen s WHERE s.theater.id = :theaterId AND s.screenNumber = :screenNumber")
    Optional<Screen> findByTheaterIdAndScreenNumber(int theaterId, int screenNumber);
    
    @Query(value = "SELECT s FROM Screen s WHERE s.theater.id = :theaterId AND s.name = :name")
    Optional<Screen> findByTheaterIdAndName(int theaterId, String name);
}

