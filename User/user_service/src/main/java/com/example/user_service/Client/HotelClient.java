package com.example.user_service.Client;

import com.example.user_service.Dto.HotelResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hotel-service")
public interface HotelClient {

    @GetMapping("/hotels/{hotelId}/details")
    HotelResponse getHotelById(@PathVariable("hotelId") String hotelId);
}