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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/")
public class UserController {
    @Autowired
    UserServ UserServ;

    @GetMapping("users")
    public List<user> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        try {
            return List.of(UserServ.getUserById(jwt.getSubject()));
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }
    @PostMapping("adduser")
    public ResponseEntity<String> addUser(@RequestBody user user, @AuthenticationPrincipal Jwt jwt) {
        try {
            user.setUserid(jwt.getSubject());
            UserServ.addUser(user);
            return ResponseEntity.ok("User added successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("users/{userid}")
    public ResponseEntity<UserWithRatingsResponse> getUserById(
            @PathVariable String userid,
            @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userid, jwt);
        try {
            return ResponseEntity.ok(UserServ.getUserWithRatings(userid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("users/{userid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userid, @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userid, jwt);
        try {
            UserServ.deleteUser(userid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static void requireSelf(String userId, Jwt jwt) {
        if (!jwt.getSubject().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own user profile");
        }
    }

}
