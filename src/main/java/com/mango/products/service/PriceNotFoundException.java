package com.mango.products.service;

import java.time.LocalDate;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(Long productId, LocalDate date) {
        super("No price in effect for product " + productId + " on " + date);
    }
}
