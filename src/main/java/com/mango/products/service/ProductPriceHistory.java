package com.mango.products.service;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;

import java.util.List;

public record ProductPriceHistory(Product product, List<Price> prices) {
}
