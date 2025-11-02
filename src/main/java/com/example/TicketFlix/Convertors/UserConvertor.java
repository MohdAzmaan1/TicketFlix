package com.example.TicketFlix.Convertors;

import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Models.User;

public class UserConvertor {

    //Static is kept to avoid calling it via objects/instances
    // Note: Password should be encrypted before calling this method
    public static User convertDtoToEntity(UserEntryDTO userEntryDTO){
        User.UserBuilder builder = User.builder()
                .age(userEntryDTO.getAge())
                .address(userEntryDTO.getAddress())
                .email(userEntryDTO.getEmail())
                .mobileNumber(userEntryDTO.getMobileNumber())
                .name(userEntryDTO.getName());

        // Password should already be encrypted when passed here
        if (userEntryDTO.getPassword() != null) {
            builder.password(userEntryDTO.getPassword());
        }

        // Role defaults to USER if not specified
        if (userEntryDTO.getRole() != null) {
            builder.role(userEntryDTO.getRole());
        } else {
            builder.role(User.UserRole.USER);
        }

        builder.enabled(true); // New users are enabled by default

        return builder.build();
    }

    public static UserEntryDTO convertEntityToDto(User user){
        UserEntryDTO userEntryDTO = new UserEntryDTO();
        userEntryDTO.setAge(user.getAge());
        userEntryDTO.setAddress(user.getAddress());
        userEntryDTO.setEmail(user.getEmail());
        userEntryDTO.setMobileNumber(user.getMobileNumber());
        userEntryDTO.setName(user.getName());
        // Note: Password and role are NOT returned in DTO for security
        return userEntryDTO;
    }
}
