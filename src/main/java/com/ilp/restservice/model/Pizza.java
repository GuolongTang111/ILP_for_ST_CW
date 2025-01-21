package com.ilp.restservice.model;

public class Pizza {
    private String name;
    private int priceInPence;

    public Pizza() {
    }

    public Pizza(String name, int priceInPence) {
        this.name = name;
        this.priceInPence = priceInPence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriceInPence() {
        return priceInPence;
    }

    public void setPriceInPence(int priceInPence) {
        this.priceInPence = priceInPence;
    }
}
