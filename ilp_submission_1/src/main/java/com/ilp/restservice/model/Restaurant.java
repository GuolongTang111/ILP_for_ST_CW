package com.ilp.restservice.model;

import java.util.List;

public class Restaurant {
    private String name;
    private Position location;
    private List<String> openingDays;
    private List<Pizza> menu;

    public Restaurant() {
    }

    public Restaurant(String name, Position location, List<String> openingDays, List<Pizza> menu) {
        this.name = name;
        this.location = location;
        this.openingDays = openingDays;
        this.menu = menu;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Position getLocation() {
        return location;
    }
    public void setLocation(Position location) {
        this.location = location;
    }

    public List<String> getOpeningDays() {
        return openingDays;
    }
    public void setOpeningDays(List<String> openingDays) {
        this.openingDays = openingDays;
    }

    public List<Pizza> getMenu() {
        return menu;
    }
    public void setMenu(List<Pizza> menu) {
        this.menu = menu;
    }
}
