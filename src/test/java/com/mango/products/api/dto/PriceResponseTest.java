package com.mango.products.api.dto;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PriceResponseTest {

    @Test
    void fromMapsAllFieldsFromPrice() {
        Product product = new Product("Shirt", "desc");
        Price price = new Price(product, BigDecimal.valueOf(19.99), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        PriceResponse response = PriceResponse.from(price);

        assertThat(response.id()).isEqualTo(price.getId());
        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.value()).isEqualByComparingTo("19.99");
        assertThat(response.initDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }
}
