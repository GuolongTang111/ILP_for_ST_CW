package com.ilp.restservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // or javax.annotation.PostConstruct

import com.ilp.restservice.model.Restaurant;

import jakarta.annotation.PostConstruct;

@Service
public class RestaurantFetchService {

    private final RestTemplate restTemplate;
    private List<Restaurant> cachedRestaurants;

    private static final String RESTAURANTS_URL = "https://ilp-rest-2024.azurewebsites.net/restaurants";

    public RestaurantFetchService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        this.cachedRestaurants = fetchRestaurantsFromApi(RESTAURANTS_URL);
    }

    public List<Restaurant> fetchRestaurantsFromApi(String url) {
        Restaurant[] arr = restTemplate.getForObject(url, Restaurant[].class);
        if (arr == null) {
            return List.of();
        }
        return Arrays.asList(arr);
    }

    public List<Restaurant> getAllRestaurants() {
        return cachedRestaurants; // Already fetched
    }

    // Optional: method to refresh data if needed
    public void refresh() {
        this.cachedRestaurants = fetchRestaurantsFromApi(RESTAURANTS_URL);
    }
}
