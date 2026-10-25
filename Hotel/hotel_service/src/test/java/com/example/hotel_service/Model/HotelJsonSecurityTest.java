package com.example.hotel_service.Model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class HotelJsonSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void clientsCannotSetHotelIdOrReadOwnerSubject() throws Exception {
        Hotel hotel = objectMapper.readValue(
                "{\"hotel_id\":\"another-owner-hotel\",\"name\":\"New stay\"}",
                Hotel.class);
        hotel.setOwnerSubject("google-owner-subject");

        assertNull(hotel.getHotelId());
        JsonNode response = objectMapper.readTree(objectMapper.writeValueAsString(hotel));
        assertFalse(response.has("ownerSubject"));
        assertFalse(response.has("owner_subject"));
    }
}
