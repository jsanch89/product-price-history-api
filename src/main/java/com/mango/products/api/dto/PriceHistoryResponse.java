package com.mango.products.api.dto;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;

import java.util.List;

public record PriceHistoryResponse(ProductResponse product, List<PriceResponse> prices) {

    public static PriceHistoryResponse from(Product product, List<Price> prices) {
        return new PriceHistoryResponse(
                ProductResponse.from(product),
                prices.stream().map(PriceResponse::from).toList());
    }
}
