package com.example.rating_service.Controller;

import com.example.rating_service.Model.Rating;
import com.example.rating_service.Service.RatingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<Rating> createRating(@RequestBody Rating rating) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(rating));
    }

    @GetMapping("/{ratingId}")
    public ResponseEntity<Rating> getRatingByRatingId(@PathVariable String ratingId) {
        return ResponseEntity.ok(ratingService.getRatingByRatingId(ratingId));
    }

    @GetMapping("/user/{userid}")
    public ResponseEntity<List<Rating>> getRatingsByUserid(@PathVariable String userid) {
        return ResponseEntity.ok(ratingService.getRatingsByUserid(userid));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<Rating>> getRatingsByHotelId(@PathVariable String hotelId) {
        return ResponseEntity.ok(ratingService.getRatingsByHotelId(hotelId));
    }

    @GetMapping
    public ResponseEntity<List<Rating>> getAllRatings() {
        return ResponseEntity.ok(ratingService.getAllRatings());
    }

    @DeleteMapping("/{ratingId}")
    public ResponseEntity<Void> deleteRatingByRatingId(@PathVariable String ratingId) {
        ratingService.deleteRatingByRatingId(ratingId);
        return ResponseEntity.noContent().build();
    }
}