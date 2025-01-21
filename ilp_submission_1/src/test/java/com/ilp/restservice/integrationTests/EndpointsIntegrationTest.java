package com.ilp.restservice.integrationTests;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilp.restservice.model.Order;
import com.ilp.restservice.testdto.TestOrderDTO;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For serializing/deserializing JSON

    private static final String TEST_ORDERS_URL = "https://ilp-rest-2024.azurewebsites.net/orders";

    private List<TestOrderDTO> testOrders; // We'll store the "test" orders from that URL

    @BeforeAll
    void setUpTestData() {
        // 1) Fetch the test orders (which have orderStatus/validationCode)
        RestTemplate restTemplate = new RestTemplate();
        TestOrderDTO[] arr = restTemplate.getForObject(TEST_ORDERS_URL, TestOrderDTO[].class);
        if (arr != null) {
            testOrders = Arrays.asList(arr);
        }
    }

    @Test
    @DisplayName("Test endpoints using the TestOrderDTO data")
    void testEndpoints() throws Exception {
        if (testOrders == null || testOrders.isEmpty()) {
            System.out.println("No test orders fetched from " + TEST_ORDERS_URL);
            return;
        }

        for (TestOrderDTO testOrder : testOrders) {
            System.out.println("Testing orderNo: " + testOrder.getOrderNo()
                    + " (expect " + testOrder.getOrderStatus() + ", code " + testOrder.getOrderValidationCode() + ")");

            // 2) Convert TestOrderDTO -> real Order object
            Order realOrder = mapToRealOrder(testOrder);

            // 3) Validate Order
            testValidateOrder(realOrder, testOrder);

            // 4) calcDeliveryPath
            testCalcDeliveryPath(realOrder, testOrder);

            // 5) calcDeliveryPathAsGeoJson
            testCalcDeliveryPathAsGeoJson(realOrder, testOrder);
        }
    }

    // ----------------------------
    // Helper methods
    // ----------------------------

    /**
     * Build a real Order from the testOrderDTO, ignoring the status & validation code fields
     */
    private Order mapToRealOrder(TestOrderDTO testOrder) {
        Order real = new Order();
        real.setOrderNo(testOrder.getOrderNo());
        real.setOrderDate(testOrder.getOrderDate());
        real.setPriceTotalInPence(testOrder.getPriceTotalInPence());
        real.setPizzasInOrder(testOrder.getPizzasInOrder());
        real.setCreditCardInformation(testOrder.getCreditCardInformation());
        return real;
    }

    /**
     * Calls /validateOrder. 
     * Expects 200, then checks JSON fields "orderStatus" & "orderValidationCode" 
     * against the ones from TestOrderDTO.
     */
    private void testValidateOrder(Order realOrder, TestOrderDTO testOrder) throws Exception {
        String json = objectMapper.writeValueAsString(realOrder);

        // We expect your endpoint to return 200 even for "INVALID" orders, 
        // with a JSON body containing { orderStatus, orderValidationCode } 
        // so we can compare them:

        mockMvc.perform(post("/validateOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value(testOrder.getOrderStatus()))
                .andExpect(jsonPath("$.orderValidationCode").value(testOrder.getOrderValidationCode()));
    }

    /**
     * Calls /calcDeliveryPath. 
     * If the order is expected INVALID, we expect 400. If VALID, expect 200.
     * Also measures the execution time to ensure it finishes within 5 seconds.
     */
    private void testCalcDeliveryPath(Order realOrder, TestOrderDTO testOrder) throws Exception {
        String json = objectMapper.writeValueAsString(realOrder);

        boolean isValid = "VALID".equalsIgnoreCase(testOrder.getOrderStatus());

        long startTime = System.currentTimeMillis();
        mockMvc.perform(post("/calcDeliveryPath")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(isValid ? status().isOk() : status().isBadRequest());
        long endTime = System.currentTimeMillis();

        long durationMs = endTime - startTime;
        assertTrue(durationMs < 5000, 
            String.format("/calcDeliveryPath took %d ms, which is >= 5000 ms", durationMs));
    }

    /**
     * Calls /calcDeliveryPathAsGeoJson. 
     * If the order is expected INVALID, we expect 400. If VALID, expect 200 
     * and a GeoJSON FeatureCollection.
     * Also measures the execution time to ensure it finishes within 5 seconds.
     */
    private void testCalcDeliveryPathAsGeoJson(Order realOrder, TestOrderDTO testOrder) throws Exception {
        String json = objectMapper.writeValueAsString(realOrder);

        boolean isValid = "VALID".equalsIgnoreCase(testOrder.getOrderStatus());

        long startTime = System.currentTimeMillis();
        if (isValid) {
            mockMvc.perform(post("/calcDeliveryPathAsGeoJson")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isOk())
                    // minimal checks for GeoJSON
                    .andExpect(jsonPath("$.type").value("FeatureCollection"))
                    .andExpect(jsonPath("$.features").isArray());
        } else {
            mockMvc.perform(post("/calcDeliveryPathAsGeoJson")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
        }
        long endTime = System.currentTimeMillis();

        long durationMs = endTime - startTime;
        assertTrue(durationMs < 5000, 
            String.format("/calcDeliveryPathAsGeoJson took %d ms, which is >= 5000 ms", durationMs));
    }
}
