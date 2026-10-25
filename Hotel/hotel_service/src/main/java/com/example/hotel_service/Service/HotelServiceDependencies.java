package com.example.hotel_service.Service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.hotel_service.Client.RatingClient;
import com.example.hotel_service.Dto.RatingResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Component
public class HotelServiceDependencies {

	private final RatingClient ratingClient;

	public HotelServiceDependencies(RatingClient ratingClient) {
		this.ratingClient = ratingClient;
	}

	@CircuitBreaker(name = "ratingService")
	@RateLimiter(name = "ratingService")
	public List<RatingResponse> getRatingsByHotelId(String hotelId) {
		return ratingClient.getRatingsByHotelId(hotelId);
	}
}
