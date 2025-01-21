package com.ilp.restservice.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ilp.restservice.dto.OrderValidationResult;
import com.ilp.restservice.enums.OrderStatus;
import com.ilp.restservice.model.NamedRegion;
import com.ilp.restservice.model.Order;
import com.ilp.restservice.model.Pizza;
import com.ilp.restservice.model.Position;

@Service
public class CalcDeliveryPathService {

    private static final double STEP = 0.00015;
    private static final double TOLERANCE = 0.00015;

    // Appleton Tower location
    private static final Position APPLETON_TOWER = new Position(-3.186874, 55.944494);

    // 16 directions: (dx, dy) of length STEP
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);
    private static final double[][] DIRECTIONS = {
        {0, STEP},                              // N (90°)
        {STEP * Math.cos(Math.PI / 8), STEP * Math.sin(Math.PI / 8)},   // NNE (67.5°)
        {STEP * INV_SQRT2, STEP * INV_SQRT2},   // NE (45°)
        {STEP * Math.cos(3 * Math.PI / 8), STEP * Math.sin(3 * Math.PI / 8)}, // ENE (22.5°)
        {STEP, 0},                              // E (0°)
        {STEP * Math.cos(5 * Math.PI / 8), -STEP * Math.sin(5 * Math.PI / 8)}, // ESE (-22.5°)
        {STEP * INV_SQRT2, -STEP * INV_SQRT2},  // SE (-45°)
        {STEP * Math.cos(7 * Math.PI / 8), -STEP * Math.sin(7 * Math.PI / 8)}, // SSE (-67.5°)
        {0, -STEP},                             // S (-90°)
        {-STEP * Math.cos(7 * Math.PI / 8), -STEP * Math.sin(7 * Math.PI / 8)}, // SSW (-112.5°)
        {-STEP * INV_SQRT2, -STEP * INV_SQRT2}, // SW (-135°)
        {-STEP * Math.cos(5 * Math.PI / 8), -STEP * Math.sin(5 * Math.PI / 8)}, // WSW (-157.5°)
        {-STEP, 0},                             // W (180°)
        {-STEP * Math.cos(3 * Math.PI / 8), STEP * Math.sin(3 * Math.PI / 8)},  // WNW (157.5°)
        {-STEP * INV_SQRT2, STEP * INV_SQRT2},  // NW (135°)
        {-STEP * Math.cos(Math.PI / 8), STEP * Math.sin(Math.PI / 8)}           // NNW (112.5°)
    };

    private final OrderValidationService orderValidationService;
    private final RestaurantFetchService restaurantFetchService;
    private final NoFlyZoneService noFlyZoneService;
    private final CentralAreaService centralAreaService;
    private final PointInPolygonService pointInPolygonService;

    public CalcDeliveryPathService(
            OrderValidationService orderValidationService,
            RestaurantFetchService restaurantFetchService,
            NoFlyZoneService noFlyZoneService,
            CentralAreaService centralAreaService,
            PointInPolygonService pointInPolygonService
    ) {
        this.orderValidationService = orderValidationService;
        this.restaurantFetchService = restaurantFetchService;
        this.noFlyZoneService = noFlyZoneService;
        this.centralAreaService = centralAreaService;
        this.pointInPolygonService = pointInPolygonService;
    }

    /**
     * Main entry: compute the path from the restaurant to Appleton Tower using A*.
     * - Validate order
     * - Find restaurant location
     * - Run A*
     * - Insert hover steps
     * - Return the resulting path
     *
     * @param order The incoming order
     * @return The path as a list of Position objects
     * @throws IllegalArgumentException if order is invalid or no path can be found
     */
    public List<Position> computeDeliveryPath(Order order) {
        // 1) Validate order first
        OrderValidationResult validationResult = orderValidationService.validateOrder(order);
        if (validationResult.getOrderStatus() != OrderStatus.VALID) {
            // If not valid, throw 400
            throw new IllegalArgumentException("Order invalid: " + validationResult.getOrderValidationCode());
        }

        // 2) Get restaurant location (all pizzas must come from exactly one restaurant)
        Position restaurantPos = findRestaurantLocation(order);

        // 3) A* from restaurant to Appleton Tower
        List<Position> rawPath = aStarSearch(restaurantPos, APPLETON_TOWER);

        if (rawPath.isEmpty()) {
            throw new IllegalArgumentException("No path found (A* search returned empty).");
        }

        // 4) Insert hover steps:
        //    - Duplicate the first coordinate (hover at restaurant)
        //    - Duplicate the last coordinate (hover at Appleton Tower)
        List<Position> finalPath = new ArrayList<>(rawPath);
        finalPath.add(1, rawPath.get(0));
        finalPath.add(finalPath.get(finalPath.size() - 1));

        return finalPath;
    }

    /**
     * Attempt to find exactly one restaurant whose menu contains ALL the pizzas in the order.
     * If none or more than one such restaurant is found, throw an exception.
     */
    private Position findRestaurantLocation(Order order) {
        List<Pizza> pizzas = order.getPizzasInOrder();
        var allRestaurants = restaurantFetchService.getAllRestaurants();

        // Find a single restaurant that can fulfill ALL pizzas
        List<Position> matchingRestaurants = new ArrayList<>();
        for (var r : allRestaurants) {
            boolean canFulfillAll = true;
            for (Pizza p : pizzas) {
                boolean found = r.getMenu().stream()
                        .anyMatch(menuPizza -> menuPizza.getName().equalsIgnoreCase(p.getName()));
                if (!found) {
                    canFulfillAll = false;
                    break;
                }
            }
            if (canFulfillAll) {
                matchingRestaurants.add(new Position(r.getLocation().getLng(), r.getLocation().getLat()));
            }
        }

        if (matchingRestaurants.size() == 1) {
            return matchingRestaurants.get(0);
        } else {
            throw new IllegalArgumentException(
                    "Could not find a single restaurant that can supply all pizzas for this order."
            );
        }
    }

    /**
     * A* search from start to goal. Return the path as a list of Position.
     */
    private List<Position> aStarSearch(Position start, Position goal) {

        // We'll track whether we're inside the central area in our Node
        boolean startInCentral = isInsideCentral(start);

        Node startNode = new Node(start, 0.0, heuristicDistance(start, goal), null, startInCentral);

        // Min-heap on f = g + h
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f()));
        openSet.add(startNode);

        // Keep track of visited states: (lng, lat, insideCentral)
        Set<NodeKey> visited = new HashSet<>();
        visited.add(new NodeKey(start, startInCentral));

        Map<NodeKey, Node> cameFrom = new HashMap<>();
        cameFrom.put(new NodeKey(start, startInCentral), startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // If we're within TOLERANCE of the goal, reconstruct path
            if (distance(current.position, goal) < TOLERANCE) {
                return reconstructPath(current);
            }

            // Explore neighbors
            for (double[] dir : DIRECTIONS) {
                Position nextPos = new Position(
                        current.position.getLng() + dir[0],
                        current.position.getLat() + dir[1]
                );
                // Next node's insideCentral depends on either we were already inside OR we just stepped inside
                boolean nextInCentral = current.insideCentralArea || isInsideCentral(nextPos);

                // Check if this move is valid
                if (!isValidMove(current.position, nextPos, current.insideCentralArea, nextInCentral)) {
                    continue; // skip invalid neighbor
                }

                double tentativeG = current.g + 1.0; // each step costs 1

                NodeKey nextKey = new NodeKey(nextPos, nextInCentral);
                if (!visited.contains(nextKey)) {
                    visited.add(nextKey);

                    // Heuristic
                    double hVal = heuristicDistance(nextPos, goal);

                    Node nextNode = new Node(nextPos, tentativeG, hVal, current, nextInCentral);
                    openSet.add(nextNode);
                    cameFrom.put(nextKey, nextNode);
                }
            }
        }

        // No path found
        return List.of();
    }

    /**
     * Reconstruct path by following parent links up to the start.
     */
    private List<Position> reconstructPath(Node endNode) {
        List<Position> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        Collections.reverse(path); // because we went from end to start
        return path;
    }

    /**
     * Checks if a proposed move from `startPos` to `endPos` is valid:
     * 1) Must not cross no-fly zones.
     * 2) If we were in central area, we can't leave. (onceInside -> alwaysInside).
     * 3) Otherwise, do your usual checks (e.g. bounding box or other constraints).
     */
    private boolean isValidMove(Position startPos, Position endPos, boolean wasInsideCentral, boolean isInsideCentralNow) {
        // 1) If we were inside central area, we can't leave:
        if (wasInsideCentral && !isInsideCentralNow) {
            return false;
        }

        // 2) Check crossing or ending in a no-fly zone
        if (crossesAnyNoFlyZone(startPos, endPos)) {
            return false;
        }

        // Additional checks if needed
        return true;
    }

    /**
     * Check if the line from startPos to endPos intersects or goes inside any no-fly zone polygon.
     * Simple approach:
     *  - If the end point is inside a no-fly zone => invalid
     *  - You could also do a line-segment intersection check for each polygon if needed
     */
    private boolean crossesAnyNoFlyZone(Position startPos, Position endPos) {
        List<NamedRegion> noFlyZones = noFlyZoneService.getNoFlyZones();
        // For each zone, check:
        for (NamedRegion zone : noFlyZones) {
            // Check if end is inside => invalid
            if (pointInPolygonService.isPointInPolygon(endPos, zone.getVertices())) {
                return true;
            }
            // Optionally check the midpoint or sub-segment for robust intersection
        }
        return false;
    }

    private boolean isInsideCentral(Position pos) {
        NamedRegion central = centralAreaService.getCentralArea();
        if (central == null) {
            return false; // if no central area is defined
        }
        // Use point-in-polygon to see if pos is inside the central polygon
        return pointInPolygonService.isPointInPolygon(pos, central.getVertices());
    }

    /**
     * Heuristic = Euclidean distance from current to goal (in degrees, which is approximate).
     */
    private double heuristicDistance(Position a, Position b) {
        // We scale by STEP so we get a suitable "grid-based" heuristic
        return distance(a, b) / STEP;
    }

    /**
     * Euclidean distance in degrees (rough approximation).
     */
    private double distance(Position p1, Position p2) {
        double dx = p1.getLng() - p2.getLng();
        double dy = p1.getLat() - p2.getLat();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ----------------------------- A* Node Classes -----------------------------

    /**
     * Node representing a location in the search space.
     */
    private static class Node {
        Position position;        // the (lng, lat)
        double g;                 // cost from start
        double h;                 // heuristic distance to goal
        Node parent;              // link to reconstruct path
        boolean insideCentralArea;

        Node(Position position, double g, double h, Node parent, boolean insideCentralArea) {
            this.position = position;
            this.g = g;
            this.h = h;
            this.parent = parent;
            this.insideCentralArea = insideCentralArea;
        }

        double f() {
            // Slightly inflate the heuristic (e.g., h * 1.04) if desired
            return g + h * 1.04;
        }
    }

    /**
     * Key to track visited states: (lng, lat, insideCentral).
     */
    private static class NodeKey {
        double lng;
        double lat;
        boolean insideCentral;

        NodeKey(Position pos, boolean insideCentral) {
            this.lng = pos.getLng();
            this.lat = pos.getLat();
            this.insideCentral = insideCentral;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            NodeKey that = (NodeKey) other;
            return Double.compare(lng, that.lng) == 0
                    && Double.compare(lat, that.lat) == 0
                    && insideCentral == that.insideCentral;
        }

        @Override
        public int hashCode() {
            int result = Double.hashCode(lng);
            result = 31 * result + Double.hashCode(lat);
            result = 31 * result + (insideCentral ? 1 : 0);
            return result;
        }
    }
}
