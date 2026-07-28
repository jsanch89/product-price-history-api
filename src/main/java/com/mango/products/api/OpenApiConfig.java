package com.mango.products.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Products & Historical Prices API",
        version = "v1",
        description = "API REST para gestionar productos y sus precios historicos, sin solapamientos de vigencia."))
public class OpenApiConfig {
}
