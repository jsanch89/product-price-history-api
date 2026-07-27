package com.mango.products.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.products.api.dto.CreateProductRequest;
import com.mango.products.domain.Product;
import com.mango.products.service.ProductNotFoundException;
import com.mango.products.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void create_returnsCreatedWithLocationAndBody() throws Exception {
        Product product = new Product("Shirt", "desc");
        setId(product, 1L);
        when(productService.create("Shirt", "desc")).thenReturn(product);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest("Shirt", "desc"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Shirt"))
                .andExpect(jsonPath("$.description").value("desc"));
    }

    @Test
    void create_returnsBadRequestWhenNameBlank() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest("", "desc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsNotFoundProblemDetailWhenServiceThrows() throws Exception {
        when(productService.create(any(), any())).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest("Shirt", "desc"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: 99"));
    }

    private static void setId(Product product, Long id) throws Exception {
        Field field = Product.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(product, id);
    }
}
