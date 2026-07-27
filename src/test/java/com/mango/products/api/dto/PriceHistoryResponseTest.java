package com.mango.products.api.dto;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceHistoryResponseTest {

    @Test
    void fromMapsProductAndPrices() {
        Product product = new Product("Shirt", "desc");
        Price price = new Price(product, BigDecimal.TEN, LocalDate.of(2026, 1, 1), null);

        PriceHistoryResponse response = PriceHistoryResponse.from(product, List.of(price));

        assertThat(response.product().name()).isEqualTo("Shirt");
        assertThat(response.prices()).hasSize(1);
        assertThat(response.prices().get(0).value()).isEqualByComparingTo("10");
    }
}
