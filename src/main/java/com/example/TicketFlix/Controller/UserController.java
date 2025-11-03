package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Response.ApiResponse;
import com.example.TicketFlix.Response.ResponseFactory;
import com.example.TicketFlix.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addUser(@RequestBody UserEntryDTO userEntryDTO, HttpServletRequest request){
        try{
            String resultMessage = userService.addUser(userEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(resultMessage, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure("User not added: " + e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable int userId, HttpServletRequest request){
        try{
            String resultMessage = userService.deleteUser(userId);
            ApiResponse<Void> body = ResponseFactory.ack(resultMessage, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure("User not found or could not be deleted: " + e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<UserEntryDTO>>> getAllUsers(HttpServletRequest request){
        try{
            List<UserEntryDTO> users = userService.getAllUsers();
            ApiResponse<List<UserEntryDTO>> body = ResponseFactory.success(users, "Users fetched successfully", request);
            return new ResponseEntity<>(body, HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<List<UserEntryDTO>> body = ApiResponse.<List<UserEntryDTO>>builder()
                    .success(false)
                    .message("Failed to fetch users")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<ApiResponse<UserEntryDTO>> getUserById(@PathVariable int userId, HttpServletRequest request){
        try{
            UserEntryDTO user = userService.getUserById(userId);
            ApiResponse<UserEntryDTO> body = ResponseFactory.success(user, "User fetched successfully", request);
            return new ResponseEntity<>(body, HttpStatus.OK);
        }catch (Exception e){
            ApiResponse<UserEntryDTO> body = ApiResponse.<UserEntryDTO>builder()
                    .success(false)
                    .message("User not found")
                    .data(null)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable int userId, @RequestBody UserEntryDTO userEntryDTO, HttpServletRequest request){
        try{
            String resultMessage = userService.updateUser(userId, userEntryDTO);
            ApiResponse<Void> body = ResponseFactory.ack(resultMessage, request);
            return new ResponseEntity<>(body, HttpStatus.ACCEPTED);
        }catch (Exception e){
            ApiResponse<Void> body = ResponseFactory.failure("User not found or could not be updated: " + e.getMessage(), request);
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
    }
}
