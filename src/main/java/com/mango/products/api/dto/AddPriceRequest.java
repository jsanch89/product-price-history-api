package com.mango.products.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPriceRequest(

        @NotNull
        @PositiveOrZero
        BigDecimal value,

        @NotNull
        LocalDate initDate,

        LocalDate endDate) {

    @AssertTrue(message = "endDate must not be before initDate")
    public boolean isEndDateAfterInitDate() {
        return initDate == null || endDate == null || !endDate.isBefore(initDate);
    }
}
