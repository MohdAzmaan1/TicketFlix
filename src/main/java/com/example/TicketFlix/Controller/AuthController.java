package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.LoginRequestDTO;
import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Response.AuthResponseDTO;
import com.example.TicketFlix.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody UserEntryDTO userEntryDTO) {
        try {
            AuthResponseDTO response = authService.register(userEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            AuthResponseDTO errorResponse = AuthResponseDTO.builder()
                    .message("Registration failed: " + e.getMessage())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        try {
            AuthResponseDTO response = authService.login(loginRequestDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            AuthResponseDTO errorResponse = AuthResponseDTO.builder()
                    .message("Login failed: " + e.getMessage())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                boolean isValid = authService.validateToken(token);
                if (isValid) {
                    return new ResponseEntity<>("Token is valid", HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("Token is invalid or expired", HttpStatus.UNAUTHORIZED);
                }
            }
            return new ResponseEntity<>("Authorization header missing", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Error validating token: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


