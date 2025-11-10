package com.example.TicketFlix.Service;

import com.example.TicketFlix.EntryDTOs.LoginRequestDTO;
import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Exception.AuthenticationException;
import com.example.TicketFlix.Exception.BusinessException;
import com.example.TicketFlix.Exception.ValidationException;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.UserRepository;
import com.example.TicketFlix.Response.AuthResponseDTO;
import com.example.TicketFlix.Validation.PasswordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Autowired
    private PasswordValidator passwordValidator;

    /**
     * Register a new user
     */
    public AuthResponseDTO register(UserEntryDTO userEntryDTO) throws ValidationException, BusinessException {
        // Validate input
        validateUserRegistrationInput(userEntryDTO);

        // Validate email uniqueness
        if (StringUtils.hasText(userEntryDTO.getEmail())) {
            Optional<User> existingUser = userRepository.findByEmail(userEntryDTO.getEmail());
            if (existingUser.isPresent()) {
                throw new BusinessException("User with email " + userEntryDTO.getEmail() + " already exists");
            }
        }

        // Validate password strength
        passwordValidator.validatePassword(userEntryDTO.getPassword());

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
        String token = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());

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
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) throws ValidationException, AuthenticationException {
        // Validate input
        validateLoginInput(loginRequestDTO);

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(loginRequestDTO.getEmail());
        if (userOptional.isEmpty()) {
            throw AuthenticationException.invalidCredentials();
        }

        User user = userOptional.get();

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw AuthenticationException.accountDisabled();
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw AuthenticationException.invalidCredentials();
        }

        // Generate JWT token
        String token = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());

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

    /**
     * Validate user registration input
     */
    private void validateUserRegistrationInput(UserEntryDTO userEntryDTO) throws ValidationException {
        if (userEntryDTO == null) {
            throw new ValidationException("User data is required");
        }

        if (!StringUtils.hasText(userEntryDTO.getName())) {
            throw new ValidationException("name", "Name is required");
        }

        if (!StringUtils.hasText(userEntryDTO.getEmail())) {
            throw new ValidationException("email", "Email is required");
        }

        if (!isValidEmail(userEntryDTO.getEmail())) {
            throw new ValidationException("email", "Invalid email format");
        }

        if (StringUtils.hasText(userEntryDTO.getMobileNumber()) && !isValidMobileNumber(userEntryDTO.getMobileNumber())) {
            throw new ValidationException("mobileNumber", "Invalid mobile number format");
        }
    }

    /**
     * Validate login input
     */
    private void validateLoginInput(LoginRequestDTO loginRequestDTO) throws ValidationException {
        if (loginRequestDTO == null) {
            throw new ValidationException("Login data is required");
        }

        if (!StringUtils.hasText(loginRequestDTO.getEmail())) {
            throw new ValidationException("email", "Email is required");
        }

        if (!StringUtils.hasText(loginRequestDTO.getPassword())) {
            throw new ValidationException("password", "Password is required");
        }
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    /**
     * Validate mobile number format
     */
    private boolean isValidMobileNumber(String mobileNumber) {
        if (!StringUtils.hasText(mobileNumber)) {
            return false;
        }
        // Remove spaces, hyphens, and plus signs
        String cleanNumber = mobileNumber.replaceAll("[\\s\\-\\+]", "");
        // Check if it's 10-15 digits
        return cleanNumber.matches("^\\d{10,15}$");
    }
}

