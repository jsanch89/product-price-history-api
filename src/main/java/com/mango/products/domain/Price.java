package com.mango.products.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price")
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "init_date", nullable = false)
    private LocalDate initDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected Price() {
    }

    public Price(Product product, BigDecimal value, LocalDate initDate, LocalDate endDate) {
        if (initDate == null) {
            throw new IllegalArgumentException("initDate must not be null");
        }
        if (endDate != null && endDate.isBefore(initDate)) {
            throw new IllegalArgumentException("endDate must not be before initDate");
        }
        this.product = product;
        this.value = value;
        this.initDate = initDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDate getInitDate() {
        return initDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
