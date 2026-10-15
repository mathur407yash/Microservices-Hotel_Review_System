package com.example.user_service.Service;

import com.example.user_service.Model.user;
import com.example.user_service.Client.HotelClient;
import com.example.user_service.Client.RatingClient;
import com.example.user_service.Dto.HotelResponse;
import com.example.user_service.Dto.RatingResponse;
import com.example.user_service.Dto.RatingWithHotelResponse;
import com.example.user_service.Dto.UserWithRatingsResponse;
import com.example.user_service.Repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServ {

private final UserRepo userRepo;
private final RatingClient ratingClient;
private final HotelClient hotelClient;

public UserServ(UserRepo userRepo, RatingClient ratingClient, HotelClient hotelClient) {
    this.userRepo = userRepo;
    this.ratingClient = ratingClient;
    this.hotelClient = hotelClient;
}
public List<user> getAllUsers() {
    // Logic to retrieve all users from the database
    return userRepo.findAll();
    }

public void addUser(user user)
{
    if(user.getUserid() == null || user.getName() == null || user.getEmail() == null) {
        throw new IllegalArgumentException("User ID, Name, and Email cannot be null");
    }
    if(userRepo.existsById(user.getUserid())) {
        throw new IllegalArgumentException("User with ID " + user.getUserid() + " already exists");
    }
    userRepo.save(user);
}

public void deleteUser(String Userid){
    if(!userRepo.existsById(Userid)) {
        throw new IllegalArgumentException("User with ID " + Userid + " does not exist");
    }
    userRepo.deleteById(Userid);
}

public user getUserById(String Userid) {
    return userRepo.findById(Userid)
            .orElseThrow(() -> new IllegalArgumentException("User with ID " + Userid + " does not exist"));

}

public UserWithRatingsResponse getUserWithRatings(String userId) {
    user foundUser = getUserById(userId);
    List<RatingWithHotelResponse> ratings = ratingClient.getRatingsByUserId(userId).stream()
            .map(this::attachHotelDetails)
            .toList();
    return new UserWithRatingsResponse(foundUser, ratings);
}

private RatingWithHotelResponse attachHotelDetails(RatingResponse rating) {
    HotelResponse hotel = hotelClient.getHotelById(rating.hotelId());
    return new RatingWithHotelResponse(rating, hotel);
}
}


