package com.ilp.restservice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class NextPositionRequest {

    @NotNull
    @Valid
    private Position start;

    @NotNull
    private Double angle;

    // Getters and setters
    public Position getStart() {
        return start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    public Double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }
}
