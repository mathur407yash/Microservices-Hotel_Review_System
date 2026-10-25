package com.example.hotel_service.Repository;

import com.example.hotel_service.Model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, String> {

    List<Hotel> findByOwnerSubject(String ownerSubject);
}