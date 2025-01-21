package com.ilp.restservice.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ilp.restservice.model.NamedRegion;

import jakarta.annotation.PostConstruct;

@Service
public class NoFlyZoneService {

    private static final String NO_FLY_ZONES_URL = "https://ilp-rest-2024.azurewebsites.net/noFlyZones";

    private final RestTemplate restTemplate;
    private List<NamedRegion> noFlyZones;

    public NoFlyZoneService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        this.noFlyZones = fetchNoFlyZones();
    }

    private List<NamedRegion> fetchNoFlyZones() {
        NamedRegion[] arr = restTemplate.getForObject(NO_FLY_ZONES_URL, NamedRegion[].class);
        if (arr == null) {
            return List.of();
        }
        return Arrays.asList(arr);
    }

    public List<NamedRegion> getNoFlyZones() {
        return noFlyZones;
    }
}
