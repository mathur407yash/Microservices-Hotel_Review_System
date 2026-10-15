package com.example.user_service.Client;

import com.example.user_service.Dto.RatingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "rating-service")
public interface RatingClient {

    @GetMapping("/ratings/user/{userid}")
    List<RatingResponse> getRatingsByUserId(@PathVariable("userid") String userId);
}