package com.mango.products.service;

import com.mango.products.domain.Product;
import com.mango.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void create_savesAndReturnsProduct() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product product = productService.create("Shirt", "desc");

        assertThat(product.getName()).isEqualTo("Shirt");
        assertThat(product.getDescription()).isEqualTo("desc");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void get_returnsProductWhenFound() {
        Product product = new Product("Shirt", "desc");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.get(1L);

        assertThat(result).isSameAs(product);
    }

    @Test
    void get_throwsWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(1L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
