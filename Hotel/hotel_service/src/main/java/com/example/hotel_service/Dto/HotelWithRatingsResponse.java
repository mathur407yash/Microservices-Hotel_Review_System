package com.example.hotel_service.Dto;

import com.example.hotel_service.Model.Hotel;

import java.util.List;

public record HotelWithRatingsResponse(Hotel hotel, List<RatingResponse> ratings) {
}