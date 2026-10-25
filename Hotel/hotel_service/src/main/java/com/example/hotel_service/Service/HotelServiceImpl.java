package com.example.hotel_service.Service;

import com.example.hotel_service.Exception.HotelNotFoundException;
import com.example.hotel_service.Dto.HotelWithRatingsResponse;
import com.example.hotel_service.Model.Hotel;
import com.example.hotel_service.Repository.HotelRepository;
import org.springframework.stereotype.Service;

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
    public Hotel createHotel(Hotel hotel) {
        if (hotel.getHotelId() == null || hotel.getHotelId().isBlank()) {
            hotel.setHotelId(UUID.randomUUID().toString());
        }
        return hotelRepository.save(hotel);
    }

    @Override
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
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
    public void deleteHotelByHotelId(String hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId);
        }
        hotelRepository.deleteById(hotelId);
    }
}