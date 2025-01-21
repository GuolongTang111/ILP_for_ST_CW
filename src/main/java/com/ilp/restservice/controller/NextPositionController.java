package com.ilp.restservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ilp.restservice.model.NextPositionRequest;
import com.ilp.restservice.model.Position;
import com.ilp.restservice.service.ValidationUtils;

import jakarta.validation.Valid;

@RestController
public class NextPositionController {

    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@Valid @RequestBody NextPositionRequest request) {
        if (request == null || request.getStart() == null || request.getAngle() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Validate that the angle is within the range of 0 to 360 degrees
        if (request.getAngle() < 0 || request.getAngle() > 360) {
            return ResponseEntity.badRequest().build();
        }

        // Use the utility method to validate the position
        Position start = request.getStart();
        if (!ValidationUtils.isValidLngLat(start)) {
            return ResponseEntity.badRequest().build();
        }

        double angle = Math.toRadians(request.getAngle());

        // Calculate new position
        double newLng = start.getLng() + Math.cos(angle) * 0.00015;
        double newLat = start.getLat() + Math.sin(angle) * 0.00015;

        // Validate that the new position is also within valid range using the utility method
        Position newPosition = new Position(newLng, newLat);
        if (!ValidationUtils.isValidLngLat(newPosition)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(newPosition);
    }
}
