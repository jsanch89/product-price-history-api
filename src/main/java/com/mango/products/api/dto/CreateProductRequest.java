package com.mango.products.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(

        @Schema(description = "Product name", example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(description = "Optional product description", example = "Ergonomic wireless mouse with USB receiver")
        @Size(max = 1000)
        String description) {
}
