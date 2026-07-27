package com.mango.products.repository;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PriceRepositoryTest {

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        Product product = productRepository.save(new Product("Shirt", "desc"));
        productId = product.getId();
        priceRepository.save(new Price(product, BigDecimal.TEN,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)));
    }

    @Test
    void returnsFalseWhenNoExistingPricesForProduct() {
        boolean overlaps = priceRepository.existsOverlapping(999L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        assertThat(overlaps).isFalse();
    }

    @Test
    void returnsFalseWhenRangeIsBeforeExistingPrice() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        assertThat(overlaps).isFalse();
    }

    @Test
    void returnsFalseWhenRangeIsAfterExistingPrice() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

        assertThat(overlaps).isFalse();
    }

    @Test
    void returnsTrueWhenRangeExactlyMatchesExistingPrice() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        assertThat(overlaps).isTrue();
    }

    @Test
    void returnsTrueWhenRangePartiallyOverlapsAtStart() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 15));

        assertThat(overlaps).isTrue();
    }

    @Test
    void returnsTrueWhenRangePartiallyOverlapsAtEnd() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 7, 15));

        assertThat(overlaps).isTrue();
    }

    @Test
    void returnsTrueWhenNewRangeIsFullyContainedWithinExisting() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));

        assertThat(overlaps).isTrue();
    }

    @Test
    void returnsTrueWhenTouchingOnSingleDay() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 31));

        assertThat(overlaps).isTrue();
    }

    @Test
    void returnsTrueWhenNewRangeIsOpenEndedAndOverlaps() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 6, 1), null);

        assertThat(overlaps).isTrue();
    }

    @Test
    void returnsFalseWhenNewRangeIsOpenEndedButStartsAfterExistingPrice() {
        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2026, 7, 1), null);

        assertThat(overlaps).isFalse();
    }

    @Test
    void returnsTrueWhenExistingPriceIsOpenEnded() {
        Product product = productRepository.findById(productId).orElseThrow();
        priceRepository.save(new Price(product, BigDecimal.valueOf(15),
                LocalDate.of(2027, 1, 1), null));

        boolean overlaps = priceRepository.existsOverlapping(productId,
                LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));

        assertThat(overlaps).isTrue();
    }
}
