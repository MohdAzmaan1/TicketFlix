package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.UserConvertor;
import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    KafkaProducerService kafkaProducerService;

    @Autowired
    PasswordEncoder passwordEncoder;

    public String addUser(UserEntryDTO userEntryDTO) throws Exception, NullPointerException{
        if (userEntryDTO.getEmail() != null) {
            Optional<User> existingUser = userRepository.findByEmail(userEntryDTO.getEmail());
            if (existingUser.isPresent()) {
                throw new Exception("User with email " + userEntryDTO.getEmail() + " already exists");
            }
        }
        
        // Publish user creation event to Kafka (consumer will handle DB persistence)
        kafkaProducerService.publishUserCreationEvent(userEntryDTO);
        log.info("User creation event published to Kafka for email: {}", userEntryDTO.getEmail());
        
        return "User creation request submitted successfully";
    }

    public String deleteUser(int userId) throws Exception {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new Exception("User not found with id: " + userId);
        }
        
        // Publish user deletion event to Kafka (consumer will handle DB deletion)
        kafkaProducerService.publishUserDeletionEvent(userId);
        log.info("User deletion event published to Kafka for user ID: {}", userId);
        
        return "User deletion request submitted successfully";
    }

    public List<UserEntryDTO> getAllUsers() throws Exception {
        List<User> users = userRepository.findAll();
        // Optimized: Use Stream API for cleaner code
        return users.stream()
                .map(UserConvertor::convertEntityToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public UserEntryDTO getUserById(int userId) throws Exception {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new Exception("User not found with id: " + userId);
        }
        return UserConvertor.convertEntityToDto(userOptional.get());
    }

    public String updateUser(int userId, UserEntryDTO userEntryDTO) throws Exception {
        // Validate user exists before publishing to Kafka
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new Exception("User not found with id: " + userId);
        }
        
        // Publish user update event to Kafka (consumer will handle DB update)
        kafkaProducerService.publishUserUpdateEvent(userId, userEntryDTO);
        log.info("User update event published to Kafka for user ID: {}", userId);
        
        return "User update request submitted successfully";
    }
}
