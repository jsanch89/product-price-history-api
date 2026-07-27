package com.mango.products.service;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import com.mango.products.repository.PriceRepository;
import com.mango.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceRepository priceRepository;

    private PriceService priceService;

    @BeforeEach
    void setUp() {
        priceService = new PriceService(productRepository, priceRepository);
    }

    @Test
    void addPrice_throwsWhenProductDoesNotExist() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.addPrice(1L, BigDecimal.TEN, LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(ProductNotFoundException.class);

        verify(priceRepository, never()).save(any());
    }

    @Test
    void addPrice_throwsWhenRangeOverlapsExistingPrice() {
        Product product = new Product("Shirt", "desc");
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(priceRepository.existsOverlapping(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(true);

        assertThatThrownBy(() -> priceService.addPrice(1L, BigDecimal.TEN,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
                .isInstanceOf(PriceOverlapException.class);

        verify(priceRepository, never()).save(any());
    }

    @Test
    void addPrice_savesWhenNoOverlapAndOpenEndedRange() {
        Product product = new Product("Shirt", "desc");
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(priceRepository.existsOverlapping(1L, LocalDate.of(2026, 1, 1), null)).thenReturn(false);
        when(priceRepository.save(any(Price.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Price saved = priceService.addPrice(1L, BigDecimal.valueOf(19.99), LocalDate.of(2026, 1, 1), null);

        assertThat(saved.getEndDate()).isNull();
        assertThat(saved.getInitDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(saved.getValue()).isEqualByComparingTo("19.99");
        verify(priceRepository).save(any(Price.class));
    }

    @Test
    void getCurrentValue_throwsWhenProductDoesNotExist() {
        when(productRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> priceService.getCurrentValue(1L, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(priceRepository, never()).findCurrentValue(any(), any());
    }

    @Test
    void getCurrentValue_throwsWhenNoPriceInEffectOnDate() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(priceRepository.findCurrentValue(1L, LocalDate.of(2026, 1, 1))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.getCurrentValue(1L, LocalDate.of(2026, 1, 1)))
                .isInstanceOf(PriceNotFoundException.class);
    }

    @Test
    void getCurrentValue_returnsValueWhenPriceInEffect() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(priceRepository.findCurrentValue(1L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(BigDecimal.valueOf(19.99)));

        BigDecimal value = priceService.getCurrentValue(1L, LocalDate.of(2026, 1, 1));

        assertThat(value).isEqualByComparingTo("19.99");
    }

    @Test
    void getHistory_throwsWhenProductDoesNotExist() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.getHistory(1L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getHistory_returnsProductWithOrderedPrices() {
        Product product = new Product("Shirt", "desc");
        Price price = new Price(product, BigDecimal.TEN, LocalDate.of(2026, 1, 1), null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(priceRepository.findByProductIdOrderByInitDateAsc(1L)).thenReturn(List.of(price));

        ProductPriceHistory history = priceService.getHistory(1L);

        assertThat(history.product()).isSameAs(product);
        assertThat(history.prices()).containsExactly(price);
    }
}
