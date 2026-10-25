package com.example.user_service.Service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.user_service.Client.HotelClient;
import com.example.user_service.Client.RatingClient;
import com.example.user_service.Dto.HotelResponse;
import com.example.user_service.Dto.RatingResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Component
public class UserServiceDependencies {

	private final RatingClient ratingClient;
	private final HotelClient hotelClient;

	public UserServiceDependencies(RatingClient ratingClient, HotelClient hotelClient) {
		this.ratingClient = ratingClient;
		this.hotelClient = hotelClient;
	}

	@CircuitBreaker(name = "ratingService")
	@RateLimiter(name = "ratingService")
	public List<RatingResponse> getRatingsByUserId(String userId) {
		return ratingClient.getRatingsByUserId(userId);
	}

	@CircuitBreaker(name = "hotelService")
	@RateLimiter(name = "hotelService")
	public HotelResponse getHotelById(String hotelId) {
		return hotelClient.getHotelById(hotelId);
	}
}
