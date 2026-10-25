package com.example.hotel_service.Controller;

import com.example.hotel_service.Model.Hotel;
import com.example.hotel_service.Dto.HotelWithRatingsResponse;
import com.example.hotel_service.Service.HotelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<Hotel> createHotel(
            @RequestBody Hotel hotel,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hotelService.createHotel(hotel, jwt.getSubject()));
    }

    @GetMapping("/owned")
    public ResponseEntity<List<Hotel>> getOwnedHotels(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(hotelService.getHotelsByOwnerSubject(jwt.getSubject()));
    }

    @GetMapping
    public ResponseEntity<List<Hotel>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @GetMapping("/{hotel_id}")
    public ResponseEntity<HotelWithRatingsResponse> getHotelByHotelId(
            @PathVariable("hotel_id") String hotelId) {
        return ResponseEntity.ok(hotelService.getHotelWithRatings(hotelId));
    }

    @GetMapping("/{hotel_id}/details")
    public ResponseEntity<Hotel> getHotelDetailsByHotelId(@PathVariable("hotel_id") String hotelId) {
        return ResponseEntity.ok(hotelService.getHotelByHotelId(hotelId));
    }

    @GetMapping("/{hotel_id}/ratings")
    public ResponseEntity<HotelWithRatingsResponse> getHotelWithRatings(
            @PathVariable("hotel_id") String hotelId) {
        return ResponseEntity.ok(hotelService.getHotelWithRatings(hotelId));
    }

    @DeleteMapping("/{hotel_id}")
    public ResponseEntity<Void> deleteHotelByHotelId(
            @PathVariable("hotel_id") String hotelId,
            @AuthenticationPrincipal Jwt jwt) {
        hotelService.deleteHotelByHotelId(hotelId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}