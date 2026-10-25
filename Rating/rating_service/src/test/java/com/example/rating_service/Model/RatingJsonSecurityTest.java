package com.example.rating_service.Model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class RatingJsonSecurityTest {

    @Test
    void clientsCannotSetRatingId() throws Exception {
        Rating rating = new ObjectMapper().readValue(
                "{\"ratingId\":\"another-users-review\",\"hotelId\":\"hotel-1\",\"rating\":5}",
                Rating.class);

        assertNull(rating.getRatingId());
    }
}
