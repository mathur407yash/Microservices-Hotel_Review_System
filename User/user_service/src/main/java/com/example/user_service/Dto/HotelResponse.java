package com.example.user_service.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HotelResponse(
        @JsonProperty("hotel_id") String hotelId,
        String name,
        String location,
        String about) {
}