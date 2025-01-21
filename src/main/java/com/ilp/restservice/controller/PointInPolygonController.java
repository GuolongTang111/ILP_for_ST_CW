package com.ilp.restservice.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ilp.restservice.model.IsInRegionRequest;
import com.ilp.restservice.model.NamedRegion;
import com.ilp.restservice.model.Position;
import com.ilp.restservice.service.PointInPolygonService;
import com.ilp.restservice.service.ValidationUtils;

import jakarta.validation.Valid;

@RestController
public class PointInPolygonController {

    @Autowired
    private PointInPolygonService pointInPolygonService;

    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> checkPointInPolygon(@Valid @RequestBody IsInRegionRequest request) {
        if (request == null || request.getPosition() == null || request.getRegion() == null) {
            return ResponseEntity.badRequest().build();
        }

        Position position = request.getPosition();
        NamedRegion region = request.getRegion();
        List<Position> vertices = region.getVertices();

        // Validate the position
        if (!ValidationUtils.isValidLngLat(position)) {
            return ResponseEntity.badRequest().build();
        }

        // Check if vertices list is valid
        if (vertices == null || vertices.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate each vertex in the vertices list
        for (Position vertex : vertices) {
            if (!ValidationUtils.isValidLngLat(vertex)) {
                return ResponseEntity.badRequest().build();
            }
        }

        // Check if the first and last point of the vertices match to ensure a closed polygon
        Position firstPoint = vertices.get(0);
        Position lastPoint = vertices.get(vertices.size() - 1);
        if (!firstPoint.getLng().equals(lastPoint.getLng()) || !firstPoint.getLat().equals(lastPoint.getLat())) {
            return ResponseEntity.badRequest().build();
        }

        // Check if there are any duplicate edges in any order
        Set<Edge> edgesSet = new HashSet<>();
        for (int i = 1; i < vertices.size(); i++) {
            Position start = vertices.get(i - 1);
            Position end = vertices.get(i);

            Edge edge = new Edge(start, end);
            if (edgesSet.contains(edge)) {
                // If a duplicate edge exists, return 400
                return ResponseEntity.badRequest().build();
            }
            edgesSet.add(edge);
        }

        // Check if the point is inside the polygon
        boolean isInside = pointInPolygonService.isPointInPolygon(position, vertices);
        return ResponseEntity.ok(isInside);
    }

    // Helper class to represent an edge
    private static class Edge {
        private final Position point1;
        private final Position point2;

        public Edge(Position point1, Position point2) {
            if (point1.getLng() < point2.getLng() || 
                (point1.getLng().equals(point2.getLng()) && point1.getLat() < point2.getLat())) {
                this.point1 = point1;
                this.point2 = point2;
            } else {
                this.point1 = point2;
                this.point2 = point1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Edge other = (Edge) obj;
            return point1.getLng().equals(other.point1.getLng()) &&
                   point1.getLat().equals(other.point1.getLat()) &&
                   point2.getLng().equals(other.point2.getLng()) &&
                   point2.getLat().equals(other.point2.getLat());
        }

        @Override
        public int hashCode() {
            int result = point1.getLng().hashCode();
            result = 31 * result + point1.getLat().hashCode();
            result = 31 * result + point2.getLng().hashCode();
            result = 31 * result + point2.getLat().hashCode();
            return result;
        }
    }
}
