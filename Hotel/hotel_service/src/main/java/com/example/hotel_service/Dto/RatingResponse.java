package com.example.hotel_service.Dto;

public record RatingResponse(
        String ratingId,
        String hotelId,
        String userid,
        int rating,
        String feedback) {
}