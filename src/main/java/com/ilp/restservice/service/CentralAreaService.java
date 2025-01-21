package com.ilp.restservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ilp.restservice.model.NamedRegion;

import jakarta.annotation.PostConstruct;

@Service
public class CentralAreaService {

    private static final String CENTRAL_AREA_URL = "https://ilp-rest-2024.azurewebsites.net/centralArea";

    private final RestTemplate restTemplate;
    private NamedRegion centralArea; // single polygon

    public CentralAreaService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        this.centralArea = fetchCentralArea();
    }

    private NamedRegion fetchCentralArea() {
        // If the API returns a single region
        NamedRegion region = restTemplate.getForObject(CENTRAL_AREA_URL, NamedRegion.class);
        return region;
    }

    public NamedRegion getCentralArea() {
        return centralArea;
    }
}
