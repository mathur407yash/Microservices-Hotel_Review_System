package com.example.hotel_service.Exception;

public class HotelNotFoundException extends RuntimeException {

    public HotelNotFoundException(String hotelId) {
        super("Hotel not found with hotel_id: " + hotelId);
    }
}