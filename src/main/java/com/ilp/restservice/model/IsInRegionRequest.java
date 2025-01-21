package com.ilp.restservice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class IsInRegionRequest {

    @NotNull
    @Valid
    private Position position;

    @NotNull
    @Valid
    private NamedRegion region;

    // Getters and setters
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public NamedRegion getRegion() {
        return region;
    }

    public void setRegion(NamedRegion region) {
        this.region = region;
    }
}
