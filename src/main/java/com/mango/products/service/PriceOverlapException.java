package com.mango.products.service;

public class PriceOverlapException extends RuntimeException {

    public PriceOverlapException(Long productId) {
        super("Price range overlaps an existing price for product: " + productId);
    }
}
