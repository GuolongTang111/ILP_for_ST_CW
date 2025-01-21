package com.ilp.restservice.unitTests;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;

import com.ilp.restservice.enums.OrderValidationCode;
import com.ilp.restservice.model.CreditCardInformation;
import com.ilp.restservice.model.Order;
import com.ilp.restservice.model.Pizza;
import com.ilp.restservice.model.Restaurant;
import com.ilp.restservice.service.OrderValidationService;
import com.ilp.restservice.service.RestaurantFetchService;

/**
 * Unit tests for each sub-component method in OrderValidationService,
 * with detailed helper messages printed during each test.
 */
class OrderValidationServiceTest {

    private OrderValidationService validationService;
    private RestaurantFetchService restaurantFetchMock;

    @BeforeEach
    void setup() {
        // Mock the dependency on RestaurantFetchService
        restaurantFetchMock = Mockito.mock(RestaurantFetchService.class);
        validationService = new OrderValidationService(restaurantFetchMock);
    }

    // -------------------------------------------------------------------------
    // 1) checkBasicStructure
    // -------------------------------------------------------------------------
    @Test
    void checkBasicStructure_nullOrder() {
        // Input
        Order order = null;

        // Expected
        OrderValidationCode expected = OrderValidationCode.UNDEFINED;

        // Run
        OrderValidationCode actual = validationService.checkBasicStructure(order);

        // Print Helper Message
        System.out.println("==== Test: checkBasicStructure_nullOrder ====");
        System.out.println("Input: order = null");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   Because the order is null, we expect UNDEFINED\n");

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void checkBasicStructure_noPizzas() {
        Order order = new Order();
        order.setPizzasInOrder(null);
        order.setCreditCardInformation(new CreditCardInformation());

        OrderValidationCode expected = OrderValidationCode.UNDEFINED;
        OrderValidationCode actual = validationService.checkBasicStructure(order);

        System.out.println("==== Test: checkBasicStructure_noPizzas ====");
        System.out.println("Input: pizzasInOrder = null");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If the pizzas list is null, we expect UNDEFINED\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkBasicStructure_emptyPizzas() {
        Order order = new Order();
        order.setPizzasInOrder(Collections.emptyList());
        order.setCreditCardInformation(new CreditCardInformation());

        OrderValidationCode expected = OrderValidationCode.EMPTY_ORDER;
        OrderValidationCode actual = validationService.checkBasicStructure(order);

        System.out.println("==== Test: checkBasicStructure_emptyPizzas ====");
        System.out.println("Input: pizzasInOrder = []");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   An empty pizza list => EMPTY_ORDER\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkBasicStructure_tooManyPizzas() {
        Pizza p = new Pizza("R2: Something", 1000);
        List<Pizza> fivePizzas = List.of(p, p, p, p, p);

        Order order = new Order();
        order.setPizzasInOrder(fivePizzas);
        order.setCreditCardInformation(new CreditCardInformation());

        OrderValidationCode expected = OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED;
        OrderValidationCode actual = validationService.checkBasicStructure(order);

        System.out.println("==== Test: checkBasicStructure_tooManyPizzas ====");
        System.out.println("Input: 5 pizzas in the list");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   More than 4 pizzas => MAX_PIZZA_COUNT_EXCEEDED\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkBasicStructure_noError() {
        Order order = new Order();
        order.setPizzasInOrder(List.of(new Pizza("R1: Margarita", 1000)));
        order.setCreditCardInformation(new CreditCardInformation());

        OrderValidationCode expected = null;
        OrderValidationCode actual = validationService.checkBasicStructure(order);

        System.out.println("==== Test: checkBasicStructure_noError ====");
        System.out.println("Input: 1 pizza, valid structure");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If structure is fine, no error => null\n");

        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 2) checkAllPizzasGloballyDefined
    // -------------------------------------------------------------------------
    @Test
    void checkAllPizzasGloballyDefined_notDefined() {
        // No restaurant has "R1: Margarita"
        given(restaurantFetchMock.getAllRestaurants()).willReturn(List.of(
            new Restaurant("Some Rest", null, null,
                List.of(new Pizza("R2: AnotherPizza", 1200)))
        ));

        Order order = new Order();
        order.setPizzasInOrder(List.of(new Pizza("R1: Margarita", 1000)));

        OrderValidationCode expected = OrderValidationCode.PIZZA_NOT_DEFINED;
        OrderValidationCode actual = validationService.checkAllPizzasGloballyDefined(order);

        System.out.println("==== Test: checkAllPizzasGloballyDefined_notDefined ====");
        System.out.println("Input: 'R1: Margarita' not in any restaurant's menu");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If pizza not found in any restaurant => PIZZA_NOT_DEFINED\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkAllPizzasGloballyDefined_allGood() {
        // "R1: Margarita" found in one restaurant
        given(restaurantFetchMock.getAllRestaurants()).willReturn(List.of(
            new Restaurant("Civerinos Slice", null, null,
                List.of(new Pizza("R1: Margarita", 1000),
                        new Pizza("R1: Calzone", 1400)))
        ));

        Order order = new Order();
        order.setPizzasInOrder(List.of(new Pizza("R1: Margarita", 1000)));

        OrderValidationCode expected = null;
        OrderValidationCode actual = validationService.checkAllPizzasGloballyDefined(order);

        System.out.println("==== Test: checkAllPizzasGloballyDefined_allGood ====");
        System.out.println("Input: 'R1: Margarita' is defined in 'Civerinos Slice'");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If found globally => no error => null\n");

        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 3) findSingleRestaurantForAllPizzas
    // -------------------------------------------------------------------------
    @Test
    void findSingleRestaurantForAllPizzas_multipleRestaurants() {
        // Two restaurants can fulfill "R1: Margarita"
        Restaurant r1 = new Restaurant("Rest1", null, null,
                List.of(new Pizza("R1: Margarita", 1000)));
        Restaurant r2 = new Restaurant("Rest2", null, null,
                List.of(new Pizza("R1: Margarita", 1000)));

        given(restaurantFetchMock.getAllRestaurants()).willReturn(List.of(r1, r2));

        List<Pizza> userPizzas = List.of(new Pizza("R1: Margarita", 1000));
        Optional<Restaurant> result = validationService.findSingleRestaurantForAllPizzas(userPizzas);

        System.out.println("==== Test: findSingleRestaurantForAllPizzas_multipleRestaurants ====");
        System.out.println("Input: Two restaurants both can fulfill 'R1: Margarita'");
        System.out.println("Expected: Optional.empty()");
        System.out.println("Actual:   " + (result.isEmpty() ? "empty" : "Restaurant " + result.get().getName()));
        System.out.println("Reason:   More than one match => empty Optional\n");

        assertTrue(result.isEmpty());
    }

    @Test
    void findSingleRestaurantForAllPizzas_noneFulfill() {
        // No restaurant can fulfill "R1: Margarita"
        Restaurant r1 = new Restaurant("Rest1", null, null,
                List.of(new Pizza("R2: Another", 1200)));
        given(restaurantFetchMock.getAllRestaurants()).willReturn(List.of(r1));

        List<Pizza> userPizzas = List.of(new Pizza("R1: Margarita", 1000));
        Optional<Restaurant> result = validationService.findSingleRestaurantForAllPizzas(userPizzas);

        System.out.println("==== Test: findSingleRestaurantForAllPizzas_noneFulfill ====");
        System.out.println("Input: No restaurant contains 'R1: Margarita'");
        System.out.println("Expected: empty");
        System.out.println("Actual:   " + (result.isEmpty() ? "empty" : "Restaurant " + result.get().getName()));
        System.out.println("Reason:   None can fulfill => empty Optional\n");

        assertTrue(result.isEmpty());
    }

    @Test
    void findSingleRestaurantForAllPizzas_singleOk() {
        // Only one restaurant can fulfill these pizzas
        Restaurant r1 = new Restaurant("Rest1", null, null,
            List.of(new Pizza("R1: Margarita", 1000),
                    new Pizza("R1: Calzone", 1400)));
        given(restaurantFetchMock.getAllRestaurants()).willReturn(List.of(r1));

        List<Pizza> userPizzas = List.of(
            new Pizza("R1: Margarita", 1000),
            new Pizza("R1: Calzone", 1400)
        );
        Optional<Restaurant> result = validationService.findSingleRestaurantForAllPizzas(userPizzas);

        System.out.println("==== Test: findSingleRestaurantForAllPizzas_singleOk ====");
        System.out.println("Input: 'R1: Margarita' and 'R1: Calzone' both in 'Rest1'");
        System.out.println("Expected: Restaurant 'Rest1'");
        System.out.println("Actual:   " + (result.isPresent() ? result.get().getName() : "empty"));
        System.out.println("Reason:   Exactly one match => return that restaurant\n");

        assertTrue(result.isPresent());
        assertEquals("Rest1", result.get().getName());
    }

    // -------------------------------------------------------------------------
    // 4) checkRestaurantIsOpen
    // -------------------------------------------------------------------------
    @Test
    void checkRestaurantIsOpen_closed() {
        Restaurant r = new Restaurant(); // no openingDays => closed
        LocalDate date = LocalDate.of(2025,1,1); // e.g. Wednesday

        OrderValidationCode expected = OrderValidationCode.RESTAURANT_CLOSED;
        OrderValidationCode actual = validationService.checkRestaurantIsOpen(r, date);

        System.out.println("==== Test: checkRestaurantIsOpen_closed ====");
        System.out.println("Input: No openingDays, date=2025-01-01");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If restaurant has no days => closed\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkRestaurantIsOpen_open() {
        Restaurant r = new Restaurant("SomeName", null,
                List.of("WEDNESDAY","THURSDAY"),
                List.of());
        LocalDate date = LocalDate.of(2025,1,1); // Wednesday

        OrderValidationCode actual = validationService.checkRestaurantIsOpen(r, date);

        System.out.println("==== Test: checkRestaurantIsOpen_open ====");
        System.out.println("Input: openingDays=[WEDNESDAY,THURSDAY], date=Wed");
        System.out.println("Expected: null (no error)");
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   The restaurant is open on Wed => no error\n");

        assertNull(actual);
    }

    @Test
    void checkRestaurantIsOpen_dateNull() {
        // date is null => skip check => no error
        Restaurant r = new Restaurant();
        OrderValidationCode actual = validationService.checkRestaurantIsOpen(r, null);

        System.out.println("==== Test: checkRestaurantIsOpen_dateNull ====");
        System.out.println("Input: date=null");
        System.out.println("Expected: null");
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If date is null, the code returns no error\n");

        assertNull(actual);
    }

    // -------------------------------------------------------------------------
    // 5) checkEachPizzaPrice
    // -------------------------------------------------------------------------
    @Test
    void checkEachPizzaPrice_mismatch() {
        Restaurant r = new Restaurant("Rest", null, null,
            List.of(new Pizza("R1: Margarita", 1000)));
        List<Pizza> userPizzas = List.of(
            new Pizza("R1: Margarita", 1200) // mismatch
        );

        OrderValidationCode expected = OrderValidationCode.PRICE_FOR_PIZZA_INVALID;
        OrderValidationCode actual = validationService.checkEachPizzaPrice(r, userPizzas, 0);

        System.out.println("==== Test: checkEachPizzaPrice_mismatch ====");
        System.out.println("Input: official=1000, user=1200");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   Price mismatch => PRICE_FOR_PIZZA_INVALID\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkEachPizzaPrice_noError() {
        Restaurant r = new Restaurant("Rest", null, null,
            List.of(new Pizza("R1: Margarita", 1000)));
        List<Pizza> userPizzas = List.of(
            new Pizza("R1: Margarita", 1000)
        );

        OrderValidationCode actual = validationService.checkEachPizzaPrice(r, userPizzas, 0);

        System.out.println("==== Test: checkEachPizzaPrice_noError ====");
        System.out.println("Input: official=1000, user=1000 => match");
        System.out.println("Expected: null (no error)");
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If user price matches official => no error\n");

        assertNull(actual);
    }

    // -------------------------------------------------------------------------
    // computeOfficialTotal
    // -------------------------------------------------------------------------
    @Test
    void computeOfficialTotal_simple() {
        Restaurant r = new Restaurant("Rest", null, null,
            List.of(
                new Pizza("R1: Margarita", 1000),
                new Pizza("R1: Calzone", 1400)
            )
        );
        List<Pizza> userPizzas = List.of(
            new Pizza("R1: Margarita", 1000),
            new Pizza("R1: Calzone", 1400)
        );

        int actual = validationService.computeOfficialTotal(r, userPizzas);

        System.out.println("==== Test: computeOfficialTotal_simple ====");
        System.out.println("Input: 'R1: Margarita'(1000), 'R1: Calzone'(1400)");
        System.out.println("Expected: 2400");
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   1000 + 1400 => 2400\n");

        assertEquals(2400, actual);
    }

    // -------------------------------------------------------------------------
    // 6) checkFinalTotal
    // -------------------------------------------------------------------------
    @Test
    void checkFinalTotal_incorrect() {
        int official = 2000;
        int user = 2101;

        OrderValidationCode expected = OrderValidationCode.TOTAL_INCORRECT;
        OrderValidationCode actual = validationService.checkFinalTotal(official, user);

        System.out.println("==== Test: checkFinalTotal_incorrect ====");
        System.out.println("Input: official=2000 => official+100=2100, user=2101");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   2101 != 2100 => mismatch\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkFinalTotal_correct() {
        int official = 2300;
        int user = 2400; // 2300 + 100 => 2400

        OrderValidationCode actual = validationService.checkFinalTotal(official, user);

        System.out.println("==== Test: checkFinalTotal_correct ====");
        System.out.println("Input: official=2300 => 2300+100=2400, user=2400");
        System.out.println("Expected: null");
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If they match => no error\n");

        assertNull(actual);
    }

    // -------------------------------------------------------------------------
    // 7) checkCreditCard
    // -------------------------------------------------------------------------
    @Test
    void checkCreditCard_invalidCardNumber() {
        CreditCardInformation cci = new CreditCardInformation("1234", "12/25", "123");

        OrderValidationCode expected = OrderValidationCode.CARD_NUMBER_INVALID;
        OrderValidationCode actual = validationService.checkCreditCard(cci);

        System.out.println("==== Test: checkCreditCard_invalidCardNumber ====");
        System.out.println("Input: 4-digit card number");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   Card number <16 => invalid\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkCreditCard_expired() {
        CreditCardInformation cci = new CreditCardInformation("1234567812345678","01/21","123");
        // Suppose 21 is in the past

        OrderValidationCode expected = OrderValidationCode.EXPIRY_DATE_INVALID;
        OrderValidationCode actual = validationService.checkCreditCard(cci);

        System.out.println("==== Test: checkCreditCard_expired ====");
        System.out.println("Input: expiry=01/21 => expired");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   If card is past expiry => invalid\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkCreditCard_invalidCVV() {
        CreditCardInformation cci = new CreditCardInformation("1234567812345678","12/25","12");

        OrderValidationCode expected = OrderValidationCode.CVV_INVALID;
        OrderValidationCode actual = validationService.checkCreditCard(cci);

        System.out.println("==== Test: checkCreditCard_invalidCVV ====");
        System.out.println("Input: CVV length=2");
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   CVV must be 3 digits\n");

        assertEquals(expected, actual);
    }

    @Test
    void checkCreditCard_ok() {
        CreditCardInformation cci = new CreditCardInformation("1234567812345678","12/25","123");

        OrderValidationCode actual = validationService.checkCreditCard(cci);

        System.out.println("==== Test: checkCreditCard_ok ====");
        System.out.println("Input: 16-digit, future expiry, 3-digit CVV");
        System.out.println("Expected: null");
        System.out.println("Actual:   " + actual);
        System.out.println("Reason:   All fields valid => no error\n");

        assertNull(actual);
    }

    // -------------------------------------------------------------------------
    // Additional small checks for isCardNumberValid, etc.
    // -------------------------------------------------------------------------
    @Test
    void isCardNumberValidTest() {
        // null => false
        boolean result1 = validationService.isCardNumberValid(null);
        System.out.println("==== Test: isCardNumberValidTest: null ====");
        System.out.println("Input: null");
        System.out.println("Expected: false");
        System.out.println("Actual:   " + result1);
        System.out.println("Reason:   Null => not 16 digits => false\n");
        assertFalse(result1);

        // length <16 => false
        boolean result2 = validationService.isCardNumberValid("12345678");
        System.out.println("==== Test: isCardNumberValidTest: short number ====");
        System.out.println("Input: '12345678' => length=8");
        System.out.println("Expected: false");
        System.out.println("Actual:   " + result2);
        System.out.println("Reason:   Must be 16 => false\n");
        assertFalse(result2);

        // exactly 16 => true
        boolean result3 = validationService.isCardNumberValid("1111222233334444");
        System.out.println("==== Test: isCardNumberValidTest: 16 digits ====");
        System.out.println("Input: '1111222233334444' => length=16");
        System.out.println("Expected: true");
        System.out.println("Actual:   " + result3);
        System.out.println("Reason:   16 digits => true\n");
        assertTrue(result3);
    }
}
