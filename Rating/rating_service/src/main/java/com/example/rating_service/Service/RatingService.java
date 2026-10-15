package com.example.rating_service.Service;

import com.example.rating_service.Model.Rating;

import java.util.List;

public interface RatingService {

    Rating createRating(Rating rating);

    Rating getRatingByRatingId(String ratingId);

    List<Rating> getRatingsByUserid(String userid);

    List<Rating> getRatingsByHotelId(String hotelId);

    List<Rating> getAllRatings();

    void deleteRatingByRatingId(String ratingId);
}