package com.mango.products.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.products.api.dto.AddPriceRequest;
import com.mango.products.domain.Price;
import com.mango.products.domain.Product;
import com.mango.products.service.PriceNotFoundException;
import com.mango.products.service.PriceOverlapException;
import com.mango.products.service.PriceService;
import com.mango.products.service.ProductNotFoundException;
import com.mango.products.service.ProductPriceHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceController.class)
@Import(GlobalExceptionHandler.class)
class PriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PriceService priceService;

    @Test
    void addPrice_returnsCreatedWithLocationAndBody() throws Exception {
        Product product = new Product("Shirt", "desc");
        setId(product, 1L);
        Price price = new Price(product, BigDecimal.valueOf(19.99), LocalDate.of(2026, 1, 1), null);
        setId(price, 10L);
        when(priceService.addPrice(eq(1L), any(BigDecimal.class), any(LocalDate.class), any()))
                .thenReturn(price);

        AddPriceRequest request = new AddPriceRequest(BigDecimal.valueOf(19.99), LocalDate.of(2026, 1, 1), null);

        mockMvc.perform(post("/products/1/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/1/prices/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.value").value(19.99));
    }

    @Test
    void addPrice_returnsConflictWhenOverlapping() throws Exception {
        when(priceService.addPrice(any(), any(), any(), any())).thenThrow(new PriceOverlapException(1L));

        AddPriceRequest request = new AddPriceRequest(BigDecimal.TEN, LocalDate.of(2026, 1, 1), null);

        mockMvc.perform(post("/products/1/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void addPrice_returnsBadRequestWhenEndDateBeforeInitDate() throws Exception {
        AddPriceRequest request = new AddPriceRequest(BigDecimal.TEN,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));

        mockMvc.perform(post("/products/1/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentPrice_returnsValue() throws Exception {
        when(priceService.getCurrentValue(1L, LocalDate.of(2026, 1, 1)))
                .thenReturn(BigDecimal.valueOf(19.99));

        mockMvc.perform(get("/products/1/prices").param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(19.99));
    }

    @Test
    void getCurrentPrice_returnsNotFoundWhenNoPriceInEffect() throws Exception {
        when(priceService.getCurrentValue(1L, LocalDate.of(2026, 1, 1)))
                .thenThrow(new PriceNotFoundException(1L, LocalDate.of(2026, 1, 1)));

        mockMvc.perform(get("/products/1/prices").param("date", "2026-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentPrice_returnsNotFoundWhenProductMissing() throws Exception {
        when(priceService.getCurrentValue(1L, LocalDate.of(2026, 1, 1)))
                .thenThrow(new ProductNotFoundException(1L));

        mockMvc.perform(get("/products/1/prices").param("date", "2026-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHistory_returnsProductAndPrices() throws Exception {
        Product product = new Product("Shirt", "desc");
        setId(product, 1L);
        Price price = new Price(product, BigDecimal.TEN, LocalDate.of(2026, 1, 1), null);
        setId(price, 10L);
        when(priceService.getHistory(1L)).thenReturn(new ProductPriceHistory(product, List.of(price)));

        mockMvc.perform(get("/products/1/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(1))
                .andExpect(jsonPath("$.prices[0].id").value(10));
    }

    private static void setId(Product product, Long id) throws Exception {
        setField(product, "id", id);
    }

    private static void setId(Price price, Long id) throws Exception {
        setField(price, "id", id);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
