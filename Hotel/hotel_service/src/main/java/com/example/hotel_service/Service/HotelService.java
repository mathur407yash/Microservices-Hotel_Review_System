package com.example.hotel_service.Service;

import com.example.hotel_service.Model.Hotel;
import com.example.hotel_service.Dto.HotelWithRatingsResponse;

import java.util.List;

public interface HotelService {

    Hotel createHotel(Hotel hotel);

    List<Hotel> getAllHotels();

    Hotel getHotelByHotelId(String hotelId);

    HotelWithRatingsResponse getHotelWithRatings(String hotelId);

    void deleteHotelByHotelId(String hotelId);
}