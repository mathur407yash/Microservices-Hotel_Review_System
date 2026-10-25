package com.example.hotel_service.Service;

import com.example.hotel_service.Exception.HotelNotFoundException;
import com.example.hotel_service.Dto.HotelWithRatingsResponse;
import com.example.hotel_service.Model.Hotel;
import com.example.hotel_service.Repository.HotelRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final HotelServiceDependencies hotelServiceDependencies;

    public HotelServiceImpl(HotelRepository hotelRepository, HotelServiceDependencies hotelServiceDependencies) {
        this.hotelRepository = hotelRepository;
        this.hotelServiceDependencies = hotelServiceDependencies;
    }

    @Override
    public Hotel createHotel(Hotel hotel, String ownerSubject) {
        if (hotel.getHotelId() == null || hotel.getHotelId().isBlank()) {
            hotel.setHotelId(UUID.randomUUID().toString());
        }
        hotel.setOwnerSubject(ownerSubject);
        return hotelRepository.save(hotel);
    }

    @Override
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    @Override
    public List<Hotel> getHotelsByOwnerSubject(String ownerSubject) {
        return hotelRepository.findByOwnerSubject(ownerSubject);
    }

    @Override
    public Hotel getHotelByHotelId(String hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    @Override
    public HotelWithRatingsResponse getHotelWithRatings(String hotelId) {
        Hotel hotel = getHotelByHotelId(hotelId);
        return new HotelWithRatingsResponse(hotel, hotelServiceDependencies.getRatingsByHotelId(hotelId));
    }

    @Override
    public void deleteHotelByHotelId(String hotelId, String ownerSubject) {
        Hotel hotel = getHotelByHotelId(hotelId);
        if (!ownerSubject.equals(hotel.getOwnerSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage hotels you own");
        }
        hotelRepository.delete(hotel);
    }
}