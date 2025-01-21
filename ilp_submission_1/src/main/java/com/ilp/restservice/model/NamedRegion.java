package com.ilp.restservice.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class NamedRegion {

    @NotNull
    private String name;

    @NotEmpty
    @Valid
    private List<Position> vertices;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Position> getVertices() {
        return vertices;
    }

    public void setVertices(List<Position> vertices) {
        this.vertices = vertices;
    }
}
