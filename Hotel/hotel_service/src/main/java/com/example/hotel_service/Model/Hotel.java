package com.example.hotel_service.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Hotel {

    @Id
    @Column(name = "hotel_id")
    @JsonProperty(value = "hotel_id", access = JsonProperty.Access.READ_ONLY)
    private String hotelId;
    private String name;
    private String location;
    private String about;
    @Column(name = "owner_subject")
    private String ownerSubject;

    public Hotel() {
    }

    public Hotel(String hotelId, String name, String location, String about) {
        this.hotelId = hotelId;
        this.name = name;
        this.location = location;
        this.about = about;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    @JsonIgnore
    public String getOwnerSubject() {
        return ownerSubject;
    }

    public void setOwnerSubject(String ownerSubject) {
        this.ownerSubject = ownerSubject;
    }
}