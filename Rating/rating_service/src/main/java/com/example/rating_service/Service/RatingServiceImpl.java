package com.example.rating_service.Service;

import com.example.rating_service.Exception.RatingNotFoundException;
import com.example.rating_service.Model.Rating;
import com.example.rating_service.Repository.RatingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;

    public RatingServiceImpl(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Override
    public Rating createRating(Rating rating) {
        if (rating.getRatingId() == null || rating.getRatingId().isBlank()) {
            rating.setRatingId(UUID.randomUUID().toString());
        }
        return ratingRepository.save(rating);
    }

    @Override
    public Rating getRatingByRatingId(String ratingId) {
        return ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RatingNotFoundException(ratingId));
    }

    @Override
    public List<Rating> getRatingsByUserid(String userid) {
        return ratingRepository.findByUserid(userid);
    }

    @Override
    public List<Rating> getRatingsByHotelId(String hotelId) {
        return ratingRepository.findByHotelId(hotelId);
    }

    @Override
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll();
    }

    @Override
    public void deleteRatingByRatingId(String ratingId) {
        if (!ratingRepository.existsById(ratingId)) {
            throw new RatingNotFoundException(ratingId);
        }
        ratingRepository.deleteById(ratingId);
    }
}