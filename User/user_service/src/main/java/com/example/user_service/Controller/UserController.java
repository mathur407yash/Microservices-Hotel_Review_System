package com.example.user_service.Controller;

import com.example.user_service.Model.user;
import com.example.user_service.Dto.UserWithRatingsResponse;
import com.example.user_service.Service.UserServ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
public class UserController {
    @Autowired
    UserServ UserServ;

    @GetMapping("users")
    public List<user> getAllUsers() {
        return UserServ.getAllUsers();
    }
    @PostMapping("adduser")
    public ResponseEntity<String> addUser(@RequestBody user user) {
        try {
            UserServ.addUser(user);
            return ResponseEntity.ok("User added successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("users/{userid}")
    public ResponseEntity<UserWithRatingsResponse> getUserById(@PathVariable String userid) {
        try {
            return ResponseEntity.ok(UserServ.getUserWithRatings(userid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("users/{userid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userid) {
        try {
            UserServ.deleteUser(userid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }


}
