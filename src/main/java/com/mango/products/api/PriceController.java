package com.mango.products.api;

import com.mango.products.api.dto.AddPriceRequest;
import com.mango.products.api.dto.CurrentPriceResponse;
import com.mango.products.api.dto.PriceHistoryResponse;
import com.mango.products.api.dto.PriceResponse;
import com.mango.products.domain.Price;
import com.mango.products.service.PriceService;
import com.mango.products.service.ProductPriceHistory;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/products/{productId}/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @PostMapping
    public ResponseEntity<PriceResponse> addPrice(@PathVariable Long productId,
                                                   @Valid @RequestBody AddPriceRequest request) {
        Price price = priceService.addPrice(productId, request.value(), request.initDate(), request.endDate());
        PriceResponse response = PriceResponse.from(price);
        return ResponseEntity.created(URI.create("/products/" + productId + "/prices/" + price.getId()))
                .body(response);
    }

    @GetMapping(params = "date")
    public ResponseEntity<CurrentPriceResponse> getCurrentPrice(
            @PathVariable Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BigDecimal value = priceService.getCurrentValue(productId, date);
        return ResponseEntity.ok(new CurrentPriceResponse(value));
    }

    @GetMapping
    public ResponseEntity<PriceHistoryResponse> getHistory(@PathVariable Long productId) {
        ProductPriceHistory history = priceService.getHistory(productId);
        return ResponseEntity.ok(PriceHistoryResponse.from(history.product(), history.prices()));
    }
}
