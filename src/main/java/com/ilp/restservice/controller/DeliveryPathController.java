package com.ilp.restservice.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ilp.restservice.model.Order;
import com.ilp.restservice.model.Position;
import com.ilp.restservice.service.CalcDeliveryPathService;

/**
 * Controller exposing endpoints to compute delivery paths for a drone.
 */
@RestController
public class DeliveryPathController {

    private final CalcDeliveryPathService calcDeliveryPathService;

    public DeliveryPathController(CalcDeliveryPathService calcDeliveryPathService) {
        this.calcDeliveryPathService = calcDeliveryPathService;
    }

    /**
     * Existing endpoint: compute a normal JSON list of positions
     */
    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<?> calcDeliveryPath(@RequestBody Order order) {
        try {
            List<Position> path = calcDeliveryPathService.computeDeliveryPath(order);
            return ResponseEntity.ok(path);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * New endpoint: compute the same path, but return in GeoJSON format.
     */
    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<?> calcDeliveryPathAsGeoJson(@RequestBody Order order) {
        try {
            List<Position> path = calcDeliveryPathService.computeDeliveryPath(order);

            // Convert path to GeoJSON
            Map<String, Object> geoJson = buildGeoJsonLineString(path);

            return ResponseEntity.ok(geoJson);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Helper method to convert the drone path (list of Positions) 
     * into a GeoJSON FeatureCollection with a single Feature of type LineString.
     */
    private Map<String, Object> buildGeoJsonLineString(List<Position> path) {
        // The top-level object: "FeatureCollection"
        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");

        // We'll store a list of "features"
        List<Map<String, Object>> features = new ArrayList<>();

        // One "Feature" to represent the entire path as a LineString
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");

        // The "geometry" object
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "LineString");

        // Build the list of coordinates: [[lng, lat], [lng, lat], ...]
        List<List<Double>> coordinates = new ArrayList<>();
        for (Position p : path) {
            // GeoJSON expects [longitude, latitude]
            coordinates.add(List.of(p.getLng(), p.getLat()));
        }
        geometry.put("coordinates", coordinates);

        feature.put("geometry", geometry);

        // Optionally add "properties" (meta info about the path)
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("description", "Drone path from restaurant to Appleton Tower");
        feature.put("properties", properties);

        // Add our single Feature to the list
        features.add(feature);

        // Finally, store the "features" array into the featureCollection
        featureCollection.put("features", features);

        return featureCollection;
    }
}
