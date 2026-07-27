package com.mango.products.api;

import com.mango.products.api.dto.AddPriceRequest;
import com.mango.products.api.dto.PriceResponse;
import com.mango.products.domain.Price;
import com.mango.products.service.PriceOverlapException;
import com.mango.products.service.PriceService;
import com.mango.products.service.ProductNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleProductNotFound() {
    }

    @ExceptionHandler(PriceOverlapException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handlePriceOverlap() {
    }
}
