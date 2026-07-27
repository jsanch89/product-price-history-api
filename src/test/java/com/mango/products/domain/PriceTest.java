package com.mango.products.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceTest {

    private final Product product = new Product("Shirt", "desc");

    @Test
    void allowsNullEndDate() {
        Price price = new Price(product, BigDecimal.TEN, LocalDate.of(2026, 1, 1), null);

        assertThat(price.getEndDate()).isNull();
    }

    @Test
    void allowsEndDateEqualToInitDate() {
        LocalDate date = LocalDate.of(2026, 1, 1);

        Price price = new Price(product, BigDecimal.TEN, date, date);

        assertThat(price.getInitDate()).isEqualTo(date);
        assertThat(price.getEndDate()).isEqualTo(date);
    }

    @Test
    void rejectsNullInitDate() {
        assertThatThrownBy(() -> new Price(product, BigDecimal.TEN, null, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEndDateBeforeInitDate() {
        assertThatThrownBy(() -> new Price(product, BigDecimal.TEN,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
