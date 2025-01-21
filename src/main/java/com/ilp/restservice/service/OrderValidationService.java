package com.ilp.restservice.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ilp.restservice.dto.OrderValidationResult;
import com.ilp.restservice.enums.OrderStatus;
import com.ilp.restservice.enums.OrderValidationCode;
import com.ilp.restservice.model.CreditCardInformation;
import com.ilp.restservice.model.Order;
import com.ilp.restservice.model.Pizza;
import com.ilp.restservice.model.Restaurant;

@Service
public class OrderValidationService {

    private static final int MAX_PIZZAS = 4;

    private final RestaurantFetchService restaurantFetchService;

    public OrderValidationService(RestaurantFetchService restaurantFetchService) {
        this.restaurantFetchService = restaurantFetchService;
    }

    /**
     * The main method that runs each validation step in the same order as before.
     */
    public OrderValidationResult validateOrder(Order order) {
        // 1) Basic checks
        OrderValidationCode code = checkBasicStructure(order);
        if (code != null) {
            return invalid(code);
        }

        // 2) Check all pizzas are defined in *some* restaurant
        code = checkAllPizzasGloballyDefined(order);
        if (code != null) {
            return invalid(code);
        }

        // 3) Find the *single* restaurant that can fulfill all pizzas
        Optional<Restaurant> maybeRestaurant = findSingleRestaurantForAllPizzas(order.getPizzasInOrder());
        if (maybeRestaurant.isEmpty()) {
            // means either none or more than one restaurant can fulfill them
            return invalid(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
        }
        Restaurant restaurant = maybeRestaurant.get();

        // 4) Check if the restaurant is open
        code = checkRestaurantIsOpen(restaurant, order.getOrderDate());
        if (code != null) {
            return invalid(code);
        }

        // 5) Validate each pizza price for *this* restaurant and accumulate total
        int officialTotal = 0;
        code = checkEachPizzaPrice(restaurant, order.getPizzasInOrder(), officialTotal);
        if (code != null) {
            return invalid(code);
        }
        // But we need the official total from that step => we recalc it
        officialTotal = computeOfficialTotal(restaurant, order.getPizzasInOrder());

        // 6) Compare officialTotal + 100 with user-provided price
        code = checkFinalTotal(officialTotal, order.getPriceTotalInPence());
        if (code != null) {
            return invalid(code);
        }

        // 7) Validate credit card details
        code = checkCreditCard(order.getCreditCardInformation());
        if (code != null) {
            return invalid(code);
        }

        // 8) If no issues, it's VALID + NO_ERROR
        return new OrderValidationResult(OrderStatus.VALID, OrderValidationCode.NO_ERROR);
    }

    // -------------------------------------------------------------------------
    // 1) BASIC CHECKS
    // -------------------------------------------------------------------------
    public OrderValidationCode checkBasicStructure(Order order) {
        if (order == null
                || order.getPizzasInOrder() == null
                || order.getCreditCardInformation() == null) {
            return OrderValidationCode.UNDEFINED;
        }

        List<Pizza> pizzas = order.getPizzasInOrder();
        if (pizzas.isEmpty()) {
            return OrderValidationCode.EMPTY_ORDER;
        }
        if (pizzas.size() > MAX_PIZZAS) {
            return OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED;
        }
        return null; // means no error found here
    }

    // -------------------------------------------------------------------------
    // 2) CHECK ALL PIZZAS DEFINED GLOBALLY
    // -------------------------------------------------------------------------
    public OrderValidationCode checkAllPizzasGloballyDefined(Order order) {
        for (Pizza p : order.getPizzasInOrder()) {
            if (!isPizzaInAnyRestaurant(p.getName())) {
                return OrderValidationCode.PIZZA_NOT_DEFINED;
            }
        }
        return null;
    }

    private boolean isPizzaInAnyRestaurant(String pizzaName) {
        if (pizzaName == null || pizzaName.isBlank()) {
            return false;
        }
        List<Restaurant> all = restaurantFetchService.getAllRestaurants();
        for (Restaurant r : all) {
            boolean found = r.getMenu().stream()
                    .anyMatch(menuPizza -> menuPizza.getName().equalsIgnoreCase(pizzaName));
            if (found) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // 3) FIND SINGLE RESTAURANT THAT CAN FULFILL ALL PIZZAS
    // -------------------------------------------------------------------------
    public Optional<Restaurant> findSingleRestaurantForAllPizzas(List<Pizza> pizzas) {
        List<Restaurant> allRestaurants = restaurantFetchService.getAllRestaurants();
        Restaurant candidate = null;

        for (Restaurant r : allRestaurants) {
            boolean allPizzasFound = true;
            for (Pizza userPizza : pizzas) {
                boolean found = r.getMenu().stream()
                        .anyMatch(menuPizza ->
                            menuPizza.getName().equalsIgnoreCase(userPizza.getName())
                        );
                if (!found) {
                    allPizzasFound = false;
                    break;
                }
            }
            if (allPizzasFound) {
                // If we already had a candidate, that means there's more than one
                if (candidate != null) {
                    return Optional.empty(); // multiple restaurants => empty
                }
                candidate = r;
            }
        }
        return Optional.ofNullable(candidate); // either single or null
    }

    // -------------------------------------------------------------------------
    // 4) CHECK RESTAURANT IS OPEN
    // -------------------------------------------------------------------------
    public OrderValidationCode checkRestaurantIsOpen(Restaurant restaurant, LocalDate date) {
        if (date == null) {
            // no date => original code had "skip or handle differently"
            return null;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (!isRestaurantOpenOnDay(restaurant, dow)) {
            return OrderValidationCode.RESTAURANT_CLOSED;
        }
        return null;
    }

    private boolean isRestaurantOpenOnDay(Restaurant r, DayOfWeek dow) {
        if (r.getOpeningDays() == null) {
            return false;
        }
        String dayStr = dow.name();
        return r.getOpeningDays().stream()
                .anyMatch(d -> d.equalsIgnoreCase(dayStr));
    }

    // -------------------------------------------------------------------------
    // 5) CHECK EACH PIZZA PRICE
    // -------------------------------------------------------------------------
    /**
     * Verifies each pizza's price in the restaurant’s menu. If mismatch -> error.
     * Also used to ensure the code flow remains consistent.
     */
    public OrderValidationCode checkEachPizzaPrice(Restaurant restaurant, List<Pizza> pizzas, int officialTotal) {
        for (Pizza userPizza : pizzas) {
            Optional<Pizza> menuPizza = findMenuItem(restaurant, userPizza.getName());
            if (menuPizza.isEmpty()) {
                return OrderValidationCode.PIZZA_NOT_DEFINED; // safety net
            }
            Pizza officialPizza = menuPizza.get();

            if (userPizza.getPriceInPence() != officialPizza.getPriceInPence()) {
                return OrderValidationCode.PRICE_FOR_PIZZA_INVALID;
            }
        }
        return null;
    }

    private Optional<Pizza> findMenuItem(Restaurant restaurant, String userPizzaName) {
        return restaurant.getMenu().stream()
                .filter(m -> m.getName().equalsIgnoreCase(userPizzaName))
                .findFirst();
    }

    /**
     * Actually compute the official total (sum of the real menu prices).
     */
    public int computeOfficialTotal(Restaurant restaurant, List<Pizza> userPizzas) {
        int sum = 0;
        for (Pizza userPizza : userPizzas) {
            Optional<Pizza> menuPizza = findMenuItem(restaurant, userPizza.getName());
            if (menuPizza.isPresent()) {
                sum += menuPizza.get().getPriceInPence();
            }
        }
        return sum;
    }

    // -------------------------------------------------------------------------
    // 6) CHECK FINAL TOTAL
    // -------------------------------------------------------------------------
    public OrderValidationCode checkFinalTotal(int officialTotal, int userTotal) {
        if (officialTotal + 100 != userTotal) {
            return OrderValidationCode.TOTAL_INCORRECT;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // 7) CHECK CREDIT CARD
    // -------------------------------------------------------------------------
    public OrderValidationCode checkCreditCard(CreditCardInformation cci) {
        if (!isCardNumberValid(cci.getCreditCardNumber())) {
            return OrderValidationCode.CARD_NUMBER_INVALID;
        }
        if (!isExpiryValid(cci.getCreditCardExpiry())) {
            return OrderValidationCode.EXPIRY_DATE_INVALID;
        }
        if (!isCvvValid(cci.getCvv())) {
            return OrderValidationCode.CVV_INVALID;
        }
        return null;
    }

    public boolean isCardNumberValid(String number) {
        return (number != null && number.length() == 16);
    }

    private boolean isExpiryValid(String expiry) {
        try {
            String[] parts = expiry.split("/");
            if (parts.length != 2) {
                return false;
            }
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);
            YearMonth cardYearMonth = YearMonth.of(year, month);
            // Must not expire before current month
            return !cardYearMonth.isBefore(YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCvvValid(String cvv) {
        return (cvv != null && cvv.length() == 3);
    }

    // -------------------------------------------------------------------------
    // HELPER: Return an INVALID result
    // -------------------------------------------------------------------------
    private OrderValidationResult invalid(OrderValidationCode code) {
        return new OrderValidationResult(OrderStatus.INVALID, code);
    }
}
