package com.example.hotel_service.Client;

import com.example.hotel_service.Dto.RatingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "rating-service")
public interface RatingClient {

    @GetMapping("/ratings/hotel/{hotelId}")
    List<RatingResponse> getRatingsByHotelId(@PathVariable("hotelId") String hotelId);
}