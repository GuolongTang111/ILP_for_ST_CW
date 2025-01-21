package com.ilp.restservice.testdto;

import java.time.LocalDate;
import java.util.List;

import com.ilp.restservice.model.CreditCardInformation;
import com.ilp.restservice.model.Pizza;

/**
 * A test-focused DTO that includes extra fields
 * "orderStatus" and "orderValidationCode" for verification,
 * even though the real Order class doesn't have them.
 */
public class TestOrderDTO {

    private String orderNo;
    private LocalDate orderDate;
    private String orderStatus;              // "VALID" or "INVALID"
    private String orderValidationCode;      // e.g. "CARD_NUMBER_INVALID"
    private int priceTotalInPence;
    private List<Pizza> pizzasInOrder;
    private CreditCardInformation creditCardInformation;

    public String getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }
    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderStatus() {
        return orderStatus;
    }
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderValidationCode() {
        return orderValidationCode;
    }
    public void setOrderValidationCode(String orderValidationCode) {
        this.orderValidationCode = orderValidationCode;
    }

    public int getPriceTotalInPence() {
        return priceTotalInPence;
    }
    public void setPriceTotalInPence(int priceTotalInPence) {
        this.priceTotalInPence = priceTotalInPence;
    }

    public List<Pizza> getPizzasInOrder() {
        return pizzasInOrder;
    }
    public void setPizzasInOrder(List<Pizza> pizzasInOrder) {
        this.pizzasInOrder = pizzasInOrder;
    }

    public CreditCardInformation getCreditCardInformation() {
        return creditCardInformation;
    }
    public void setCreditCardInformation(CreditCardInformation creditCardInformation) {
        this.creditCardInformation = creditCardInformation;
    }
}
