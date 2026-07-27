package com.mango.products.service;

import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import com.mango.products.repository.PriceRepository;
import com.mango.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PriceService {

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;

    public PriceService(ProductRepository productRepository, PriceRepository priceRepository) {
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
    }

    @Transactional
    public Price addPrice(Long productId, BigDecimal value, LocalDate initDate, LocalDate endDate) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (priceRepository.existsOverlapping(productId, initDate, endDate)) {
            throw new PriceOverlapException(productId);
        }

        return priceRepository.save(new Price(product, value, initDate, endDate));
    }

    public BigDecimal getCurrentValue(Long productId, LocalDate date) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }

        return priceRepository.findCurrentValue(productId, date)
                .orElseThrow(() -> new PriceNotFoundException(productId, date));
    }

    public ProductPriceHistory getHistory(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return new ProductPriceHistory(product, priceRepository.findByProductIdOrderByInitDateAsc(productId));
    }
}
