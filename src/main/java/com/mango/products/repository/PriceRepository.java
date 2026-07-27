package com.mango.products.repository;

import com.mango.products.domain.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {

    /**
     * Far-future sentinel used in place of a null end date so range comparisons
     * can be expressed with plain COALESCE instead of duplicated null checks.
     */
    LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM Price p
            WHERE p.product.id = :productId
              AND p.initDate <= COALESCE(CAST(:endDate AS date), :maxDate)
              AND COALESCE(p.endDate, :maxDate) >= :initDate
            """)
    boolean existsOverlapping(@Param("productId") Long productId,
                               @Param("initDate") LocalDate initDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("maxDate") LocalDate maxDate);

    default boolean existsOverlapping(Long productId, LocalDate initDate, LocalDate endDate) {
        return existsOverlapping(productId, initDate, endDate, FAR_FUTURE);
    }

    @Query("""
            SELECT p.value
            FROM Price p
            WHERE p.product.id = :productId
              AND p.initDate <= :date
              AND (p.endDate IS NULL OR p.endDate >= :date)
            """)
    Optional<BigDecimal> findCurrentValue(@Param("productId") Long productId, @Param("date") LocalDate date);

    List<Price> findByProductIdOrderByInitDateAsc(Long productId);
}
