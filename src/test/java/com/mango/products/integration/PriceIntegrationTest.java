package com.mango.products.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.products.api.dto.AddPriceRequest;
import com.mango.products.api.dto.CreateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PriceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullLifecycle_addCurrentAndHistory() throws Exception {
        Long productId = createProduct("Shirt", "Cotton shirt");

        addPrice(productId, "10.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(10.00));

        addPrice(productId, "12.50", LocalDate.of(2026, 7, 1), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(12.50));

        mockMvc.perform(get("/products/{id}/prices", productId).param("date", "2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(10.00));

        mockMvc.perform(get("/products/{id}/prices", productId).param("date", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(12.50));

        mockMvc.perform(get("/products/{id}/prices", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(productId))
                .andExpect(jsonPath("$.prices", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.prices[0].value").value(10.00))
                .andExpect(jsonPath("$.prices[1].value").value(12.50));
    }

    @Test
    void addPrice_boundaryDatesAreInclusive() throws Exception {
        Long productId = createProduct("Shoes", null);

        addPrice(productId, "50.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/products/{id}/prices", productId).param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(50.00));

        mockMvc.perform(get("/products/{id}/prices", productId).param("date", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(50.00));

        mockMvc.perform(get("/products/{id}/prices", productId).param("date", "2026-02-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addPrice_returnsConflictOnPartialOverlap() throws Exception {
        Long productId = createProduct("Hat", null);

        addPrice(productId, "20.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
                .andExpect(status().isCreated());

        addPrice(productId, "22.00", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30))
                .andExpect(status().isConflict());
    }

    @Test
    void addPrice_returnsConflictWhenNewRangeContainsExisting() throws Exception {
        Long productId = createProduct("Gloves", null);

        addPrice(productId, "15.00", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
                .andExpect(status().isCreated());

        addPrice(productId, "16.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
                .andExpect(status().isConflict());
    }

    @Test
    void addPrice_returnsConflictWhenExistingIsOpenEnded() throws Exception {
        Long productId = createProduct("Scarf", null);

        addPrice(productId, "8.00", LocalDate.of(2026, 1, 1), null)
                .andExpect(status().isCreated());

        addPrice(productId, "9.00", LocalDate.of(2027, 1, 1), null)
                .andExpect(status().isConflict());
    }

    @Test
    void addPrice_allowsAdjacentNonOverlappingRanges() throws Exception {
        Long productId = createProduct("Belt", null);

        addPrice(productId, "5.00", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                .andExpect(status().isCreated());

        addPrice(productId, "6.00", LocalDate.of(2026, 2, 1), null)
                .andExpect(status().isCreated());
    }

    @Test
    void addPrice_returnsBadRequestWhenEndDateBeforeInitDate() throws Exception {
        Long productId = createProduct("Jacket", null);

        addPrice(productId, "30.00", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addPrice_returnsNotFoundWhenProductMissing() throws Exception {
        addPrice(999_999L, "30.00", LocalDate.of(2026, 1, 1), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentPrice_returnsNotFoundWhenProductMissing() throws Exception {
        mockMvc.perform(get("/products/{id}/prices", 999_999L).param("date", "2026-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHistory_returnsNotFoundWhenProductMissing() throws Exception {
        mockMvc.perform(get("/products/{id}/prices", 999_999L))
                .andExpect(status().isNotFound());
    }

    private Long createProduct(String name, String description) throws Exception {
        CreateProductRequest request = new CreateProductRequest(name, description);
        MvcResult result = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private org.springframework.test.web.servlet.ResultActions addPrice(Long productId, String value,
                                                                          LocalDate initDate, LocalDate endDate) throws Exception {
        AddPriceRequest request = new AddPriceRequest(new BigDecimal(value), initDate, endDate);
        return mockMvc.perform(post("/products/{id}/prices", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
