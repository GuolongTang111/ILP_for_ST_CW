package com.ilp.restservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public class Position {

    @NotNull
    @JsonProperty("lng")
    private Double lng;

    @NotNull
    @JsonProperty("lat")
    private Double lat;

    // Default constructor
    public Position() {
    }

    // Parameterized constructor
    public Position(Double lng, Double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    // Getters and setters for lng and lat
    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }
}
