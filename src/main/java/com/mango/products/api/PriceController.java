package com.mango.products.api;

import com.mango.products.api.dto.AddPriceRequest;
import com.mango.products.api.dto.CurrentPriceResponse;
import com.mango.products.api.dto.PriceHistoryResponse;
import com.mango.products.api.dto.PriceResponse;
import com.mango.products.domain.Price;
import com.mango.products.service.PriceService;
import com.mango.products.service.ProductPriceHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Prices", description = "Historical price management")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @Operation(summary = "Add a price to a product",
            description = "Validates initDate < endDate and rejects overlaps with existing price ranges.")
    @ApiResponse(responseCode = "201", description = "Price created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Price range overlaps an existing price")
    @PostMapping
    public ResponseEntity<PriceResponse> addPrice(@PathVariable Long productId,
                                                   @Valid @RequestBody AddPriceRequest request) {
        Price price = priceService.addPrice(productId, request.value(), request.initDate(), request.endDate());
        PriceResponse response = PriceResponse.from(price);
        return ResponseEntity.created(URI.create("/products/" + productId + "/prices/" + price.getId()))
                .body(response);
    }

    @Operation(summary = "Get the price effective on a given date")
    @ApiResponse(responseCode = "200", description = "Effective price found")
    @ApiResponse(responseCode = "404", description = "Product not found, or no price is effective on that date")
    @GetMapping(params = "date")
    public ResponseEntity<CurrentPriceResponse> getCurrentPrice(
            @PathVariable Long productId,
            @Parameter(description = "Date to look up the effective price for", example = "2026-06-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BigDecimal value = priceService.getCurrentValue(productId, date);
        return ResponseEntity.ok(new CurrentPriceResponse(value));
    }

    @Operation(summary = "Get the full price history of a product")
    @ApiResponse(responseCode = "200", description = "Price history returned")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping
    public ResponseEntity<PriceHistoryResponse> getHistory(@PathVariable Long productId) {
        ProductPriceHistory history = priceService.getHistory(productId);
        return ResponseEntity.ok(PriceHistoryResponse.from(history.product(), history.prices()));
    }
}
