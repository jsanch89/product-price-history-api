package com.mango.products.api.dto;

import com.mango.products.domain.Price;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(Long id, Long productId, BigDecimal value, LocalDate initDate, LocalDate endDate) {

    public static PriceResponse from(Price price) {
        return new PriceResponse(
                price.getId(),
                price.getProduct().getId(),
                price.getValue(),
                price.getInitDate(),
                price.getEndDate());
    }
}
