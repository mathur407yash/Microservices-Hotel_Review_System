package com.example.rating_service.Exception;

public class RatingNotFoundException extends RuntimeException {

    public RatingNotFoundException(String ratingId) {
        super("Rating not found with ratingId: " + ratingId);
    }
}