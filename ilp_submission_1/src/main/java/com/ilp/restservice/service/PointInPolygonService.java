package com.ilp.restservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ilp.restservice.model.Position;

@Service
public class PointInPolygonService {

    public boolean isPointInPolygon(Position point, List<Position> vertices) {
        int intersectCount = 0;
        for (int i = 1; i < vertices.size(); i++) {
            Position v1 = vertices.get(i - 1);
            Position v2 = vertices.get(i);

            if (rayIntersectsSegment(point, v1, v2)) {
                intersectCount++;
            }

            if (isPointOnEdge(point, v1, v2)) {
                return true; // Return true if point is exactly on the edge
            }
        }
        return (intersectCount % 2) == 1;
    }

    private boolean rayIntersectsSegment(Position p, Position v1, Position v2) {
        if (v1.getLat() > v2.getLat()) {
            Position temp = v1;
            v1 = v2;
            v2 = temp;
        }

        if (p.getLat() == v1.getLat() || p.getLat() == v2.getLat()) {
            p = new Position(p.getLng(), p.getLat() + 0.0000001);
        }

        if (p.getLat() < v1.getLat() || p.getLat() > v2.getLat()) {
            return false;
        }

        if (p.getLng() > Math.max(v1.getLng(), v2.getLng())) {
            return false;
        }

        if (p.getLng() < Math.min(v1.getLng(), v2.getLng())) {
            return true;
        }

        double slope = (v2.getLng() - v1.getLng()) / (v2.getLat() - v1.getLat());
        double intersectLng = v1.getLng() + (p.getLat() - v1.getLat()) * slope;

        return p.getLng() <= intersectLng;
    }

    private boolean isPointOnEdge(Position p, Position p1, Position p2) {
        double crossProduct = (p.getLat() - p1.getLat()) * (p2.getLng() - p1.getLng()) - 
                              (p.getLng() - p1.getLng()) * (p2.getLat() - p1.getLat());
        if (Math.abs(crossProduct) > 1e-10) {
            return false;
        }

        double minX = Math.min(p1.getLng(), p2.getLng());
        double maxX = Math.max(p1.getLng(), p2.getLng());
        double minY = Math.min(p1.getLat(), p2.getLat());
        double maxY = Math.max(p1.getLat(), p2.getLat());

        return (p.getLng() >= minX && p.getLng() <= maxX) && (p.getLat() >= minY && p.getLat() <= maxY);
    }
}
