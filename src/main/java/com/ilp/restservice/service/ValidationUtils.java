package com.ilp.restservice.service;

import com.ilp.restservice.model.Position;

public class ValidationUtils {

    // Validates if a given Position has valid longitude and latitude values
    public static boolean isValidLngLat(Position position) {
        if (position == null) {
            return false;
        }
        return isValidLongitude(position.getLng()) && isValidLatitude(position.getLat());
    }

    // Helper method to validate longitude
    private static boolean isValidLongitude(Double lng) {
        return lng != null && lng >= -180.0 && lng <= 180.0;
    }

    // Helper method to validate latitude
    private static boolean isValidLatitude(Double lat) {
        return lat != null && lat >= -90.0 && lat <= 90.0;
    }
}
