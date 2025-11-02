package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Service.AuthService;
import com.example.TicketFlix.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class UserAccountController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<UserEntryDTO> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);
            
            UserEntryDTO userDTO = new UserEntryDTO();
            userDTO.setName(user.getName());
            userDTO.setEmail(user.getEmail());
            userDTO.setAge(user.getAge());
            userDTO.setMobileNumber(user.getMobileNumber());
            userDTO.setAddress(user.getAddress());
            
            return new ResponseEntity<>(userDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<String> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserEntryDTO userEntryDTO) {
        try {
            String token = authHeader.substring(7);
            User currentUser = authService.getUserFromToken(token);
            
            // Update user profile via UserService (async via Kafka)
            userService.updateUser(currentUser.getId(), userEntryDTO);
            return new ResponseEntity<>("Profile update request submitted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Change password
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('THEATER_OWNER')")
    public ResponseEntity<String> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PasswordChangeRequest request) {
        try {
            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);
            
            // Verify old password
            if (!authService.getPasswordEncoder().matches(request.getOldPassword(), user.getPassword())) {
                return new ResponseEntity<>("Old password is incorrect", HttpStatus.BAD_REQUEST);
            }
            
            // Validate new password
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return new ResponseEntity<>("New password must be at least 6 characters", HttpStatus.BAD_REQUEST);
            }
            
            // Update password via UserService (async via Kafka)
            UserEntryDTO userDTO = new UserEntryDTO();
            userDTO.setPassword(request.getNewPassword());
            userService.updateUser(user.getId(), userDTO);
            
            return new ResponseEntity<>("Password change request submitted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Inner class for password change request
    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}

