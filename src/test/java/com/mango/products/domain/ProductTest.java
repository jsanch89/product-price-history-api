package com.mango.products.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void constructorSetsNameAndDescription() {
        Product product = new Product("Shirt", "desc");

        assertThat(product.getName()).isEqualTo("Shirt");
        assertThat(product.getDescription()).isEqualTo("desc");
        assertThat(product.getId()).isNull();
    }

    @Test
    void settersUpdateNameAndDescription() {
        Product product = new Product("Shirt", "desc");

        product.setName("Pants");
        product.setDescription("new desc");

        assertThat(product.getName()).isEqualTo("Pants");
        assertThat(product.getDescription()).isEqualTo("new desc");
    }
}
