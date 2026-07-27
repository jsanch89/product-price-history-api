package com.mango.products.api.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AddPriceRequestTest {

    @Test
    void validWhenEndDateIsNull() {
        AddPriceRequest request = new AddPriceRequest(BigDecimal.TEN, LocalDate.of(2026, 1, 1), null);

        assertThat(request.isEndDateAfterInitDate()).isTrue();
    }

    @Test
    void validWhenEndDateEqualsInitDate() {
        LocalDate date = LocalDate.of(2026, 1, 1);

        AddPriceRequest request = new AddPriceRequest(BigDecimal.TEN, date, date);

        assertThat(request.isEndDateAfterInitDate()).isTrue();
    }

    @Test
    void validWhenEndDateAfterInitDate() {
        AddPriceRequest request = new AddPriceRequest(BigDecimal.TEN,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));

        assertThat(request.isEndDateAfterInitDate()).isTrue();
    }

    @Test
    void invalidWhenEndDateBeforeInitDate() {
        AddPriceRequest request = new AddPriceRequest(BigDecimal.TEN,
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1));

        assertThat(request.isEndDateAfterInitDate()).isFalse();
    }
}
