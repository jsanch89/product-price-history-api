package com.mango.products.integration;

import com.mango.products.api.dto.AddPriceRequest;
import com.mango.products.api.dto.CreateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent inserts of overlapping price ranges for the same product
 * are serialized by the pessimistic lock on the product row, so exactly one insert
 * succeeds and the rest are rejected as conflicts (never two overlapping prices persisted).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PriceConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 8;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void concurrentOverlappingInserts_onlyOneSucceeds() throws Exception {
        Long productId = createProduct();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<HttpStatus>> futures = IntStream.range(0, CONCURRENT_REQUESTS)
                    .mapToObj(i -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        AddPriceRequest request = new AddPriceRequest(
                                BigDecimal.valueOf(10 + i), LocalDate.of(2026, 1, 1), null);
                        ResponseEntity<String> response = restTemplate.postForEntity(
                                "/products/{id}/prices", request, String.class, productId);
                        return HttpStatus.valueOf(response.getStatusCode().value());
                    }))
                    .collect(Collectors.toList());

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<HttpStatus> results = new java.util.ArrayList<>();
            for (Future<HttpStatus> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            long created = results.stream().filter(s -> s == HttpStatus.CREATED).count();
            long conflicts = results.stream().filter(s -> s == HttpStatus.CONFLICT).count();

            assertThat(created).isEqualTo(1);
            assertThat(conflicts).isEqualTo(CONCURRENT_REQUESTS - 1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Long createProduct() {
        CreateProductRequest request = new CreateProductRequest("Concurrent Product", null);
        ResponseEntity<String> response = restTemplate.postForEntity("/products", request, String.class);
        String location = response.getHeaders().getLocation().toString();
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }
}
