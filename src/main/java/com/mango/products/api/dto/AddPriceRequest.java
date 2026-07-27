package com.mango.products.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPriceRequest(

        @NotNull
        @Positive
        BigDecimal value,

        @NotNull
        LocalDate initDate,

        LocalDate endDate) {

    @AssertTrue(message = "endDate must be after initDate")
    public boolean isEndDateAfterInitDate() {
        return initDate == null || endDate == null || endDate.isAfter(initDate);
    }
}
