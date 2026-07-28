package com.mango.products.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPriceRequest(

        @Schema(description = "Price value", example = "19.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        BigDecimal value,

        @Schema(description = "Date the price becomes effective (inclusive)", example = "2026-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDate initDate,

        @Schema(description = "Date the price stops being effective (inclusive); null means effective indefinitely", example = "2026-12-31")
        LocalDate endDate) {

    @AssertTrue(message = "endDate must not be before initDate")
    public boolean isEndDateAfterInitDate() {
        return initDate == null || endDate == null || !endDate.isBefore(initDate);
    }
}
