package com.example.rating_service.Repository;

import com.example.rating_service.Model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, String> {

    List<Rating> findByUserid(String userid);

    List<Rating> findByHotelId(String hotelId);
}