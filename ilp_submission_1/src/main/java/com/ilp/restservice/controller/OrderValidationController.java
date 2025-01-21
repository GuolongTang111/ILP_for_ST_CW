package com.ilp.restservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ilp.restservice.dto.OrderValidationResult;
import com.ilp.restservice.model.Order;
import com.ilp.restservice.service.OrderValidationService;

@RestController
public class OrderValidationController {

    private final OrderValidationService validationService;

    public OrderValidationController(OrderValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validateOrder")
    public ResponseEntity<OrderValidationResult> validateOrder(@RequestBody Order order) {
        OrderValidationResult result = validationService.validateOrder(order);
        return ResponseEntity.ok(result);
    }
}
