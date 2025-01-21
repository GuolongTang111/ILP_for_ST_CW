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
public class DistanceToController {

    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@Valid @RequestBody LngLatPairRequest request) {
        try {
            // Get positions from request object
            Position pos1 = request.getPosition1();
            Position pos2 = request.getPosition2();

            // Validate positions are not null
            if (pos1 == null || pos2 == null) {
                return ResponseEntity.badRequest().build();
            }

            // Validate both positions using ValidationUtils
            if (!ValidationUtils.isValidLngLat(pos1) || !ValidationUtils.isValidLngLat(pos2)) {
                return ResponseEntity.badRequest().build();
            }

            // Calculate distance using the distance formula
            double distance = Math.sqrt(
                Math.pow(pos2.getLng() - pos1.getLng(), 2) + Math.pow(pos2.getLat() - pos1.getLat(), 2)
            );

            return ResponseEntity.ok(distance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
