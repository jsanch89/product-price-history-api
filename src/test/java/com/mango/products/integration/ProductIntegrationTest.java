package com.mango.products.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.products.api.dto.CreateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProduct_returnsCreatedWithLocationAndBody() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Shirt", "Cotton shirt");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/products/\\d+")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Shirt"))
                .andExpect(jsonPath("$.description").value("Cotton shirt"));
    }

    @Test
    void createProduct_returnsBadRequestWhenNameBlank() throws Exception {
        CreateProductRequest request = new CreateProductRequest(" ", "desc");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_allowsNullDescription() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Pants", null);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.nullValue()));
    }
}
