package com.example.TicketFlix.Models;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "User")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private int age;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String mobileNumber;

    private String address;

    // Authentication fields
    @Column(nullable = false)
    private String password; // Encrypted password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER; // Default role is USER

    private boolean enabled = true; // Account enabled/disabled

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Ticket> bookedTickets = new ArrayList<>();

    public enum UserRole {
        USER,       // Regular customer
        ADMIN,      // System administrator
        THEATER_OWNER  // Theater owner
    }
}
