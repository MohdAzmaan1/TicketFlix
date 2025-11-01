package com.example.TicketFlix.Service;

import com.example.TicketFlix.Convertors.UserConvertor;
import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    KafkaProducerService kafkaProducerService;

    public String addUser(UserEntryDTO userEntryDTO) throws Exception, NullPointerException{
        User user = UserConvertor.convertDtoToEntity(userEntryDTO);

        userRepository.save(user);
        
        // Publish user registration event to Kafka
        kafkaProducerService.publishUserRegistrationEvent(user);
        log.info("User registered successfully. Event published for: {}", user.getEmail());
        
        return "User added Successfully";
    }
}
