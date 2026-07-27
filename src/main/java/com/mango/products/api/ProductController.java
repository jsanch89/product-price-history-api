package com.mango.products.api;

import com.mango.products.api.dto.CreateProductRequest;
import com.mango.products.api.dto.ProductResponse;
import com.mango.products.domain.Product;
import com.mango.products.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.create(request.name(), request.description());
        ProductResponse response = ProductResponse.from(product);
        return ResponseEntity.created(URI.create("/products/" + product.getId())).body(response);
    }
}
