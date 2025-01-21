package com.ilp.restservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ilp.restservice.model.LngLatPairRequest;
import com.ilp.restservice.model.Position;
import com.ilp.restservice.service.ValidationUtils;

import jakarta.validation.Valid;

@RestController
public class ProximityController {

    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@Valid @RequestBody LngLatPairRequest request) {
        if (request == null || request.getPosition1() == null || request.getPosition2() == null) {
            return ResponseEntity.badRequest().build();
        }

        Position pos1 = request.getPosition1();
        Position pos2 = request.getPosition2();

        // Validate that both positions have valid longitude and latitude values
        if (!ValidationUtils.isValidLngLat(pos1) || !ValidationUtils.isValidLngLat(pos2)) {
            return ResponseEntity.badRequest().build();
        }

        // Calculate distance between two positions
        double distance = Math.sqrt(Math.pow(pos2.getLng() - pos1.getLng(), 2) + Math.pow(pos2.getLat() - pos1.getLat(), 2));

        // Return true if the positions are close enough (distance < 0.00015)
        return ResponseEntity.ok(distance < 0.00015);
    }
}


