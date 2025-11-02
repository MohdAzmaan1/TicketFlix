package com.example.TicketFlix.Controller;

import com.example.TicketFlix.EntryDTOs.UserEntryDTO;
import com.example.TicketFlix.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/add")
    public ResponseEntity<String> addUser(@RequestBody UserEntryDTO userEntryDTO){

        try{
            String response = userService.addUser(userEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (Exception e){
            String result = "User not added";
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable int userId){
        try{
            String response = userService.deleteUser(userId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            String result = "User not found or could not be deleted: " + e.getMessage();
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<UserEntryDTO>> getAllUsers(){
        try{
            List<UserEntryDTO> users = userService.getAllUsers();
            return new ResponseEntity<>(users, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<UserEntryDTO> getUserById(@PathVariable int userId){
        try{
            UserEntryDTO user = userService.getUserById(userId);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable int userId, @RequestBody UserEntryDTO userEntryDTO){
        try{
            String response = userService.updateUser(userId, userEntryDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            String result = "User not found or could not be updated: " + e.getMessage();
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        }
    }
}
