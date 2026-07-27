package com.mango.products.api.dto;

import com.mango.products.domain.Product;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseTest {

    @Test
    void fromMapsAllFieldsFromProduct() throws Exception {
        Product product = new Product("Shirt", "desc");
        setId(product, 5L);

        ProductResponse response = ProductResponse.from(product);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Shirt");
        assertThat(response.description()).isEqualTo("desc");
    }

    private static void setId(Product product, Long id) throws Exception {
        Field field = Product.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(product, id);
    }
}
