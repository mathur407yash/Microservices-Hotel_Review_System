package com.example.user_service.Dto;

import com.example.user_service.Model.user;

import java.util.List;

public record UserWithRatingsResponse(user user, List<RatingWithHotelResponse> ratings) {
}