package com.example.TicketFlix.Service;

import com.example.TicketFlix.EntryDTOs.LoginRequestDTO;
import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.UserRepository;
import com.example.TicketFlix.Response.AuthResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    public AuthResponseDTO register(UserEntryDTO userEntryDTO) throws Exception {
        // Validate email uniqueness
        if (userEntryDTO.getEmail() != null) {
            Optional<User> existingUser = userRepository.findByEmail(userEntryDTO.getEmail());
            if (existingUser.isPresent()) {
                throw new Exception("User with email " + userEntryDTO.getEmail() + " already exists");
            }
        }

        // Validate password
        if (userEntryDTO.getPassword() == null || userEntryDTO.getPassword().isEmpty()) {
            throw new Exception("Password is required");
        }

        if (userEntryDTO.getPassword().length() < 6) {
            throw new Exception("Password must be at least 6 characters long");
        }

        // Create user with encrypted password
        User user = User.builder()
                .name(userEntryDTO.getName())
                .email(userEntryDTO.getEmail())
                .age(userEntryDTO.getAge())
                .mobileNumber(userEntryDTO.getMobileNumber())
                .address(userEntryDTO.getAddress())
                .password(passwordEncoder.encode(userEntryDTO.getPassword()))
                .role(userEntryDTO.getRole() != null ? userEntryDTO.getRole() : User.UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        log.info("User registered successfully: {}", user.getEmail());

        return AuthResponseDTO.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getId())
                .name(user.getName())
                .role(user.getRole().name())
                .message("Registration successful")
                .build();
    }

    /**
     * Authenticate user and generate token
     */
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) throws Exception {
        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(loginRequestDTO.getEmail());
        if (userOptional.isEmpty()) {
            throw new Exception("Invalid email or password");
        }

        User user = userOptional.get();

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new Exception("Account is disabled. Please contact administrator");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new Exception("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponseDTO.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getId())
                .name(user.getName())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    /**
     * Get user from token
     */
    public User getUserFromToken(String token) throws Exception {
        try {
            String email = jwtService.extractEmail(token);
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                throw new Exception("User not found");
            }
            return userOptional.get();
        } catch (Exception e) {
            throw new Exception("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Get password encoder (for external use)
     */
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}

