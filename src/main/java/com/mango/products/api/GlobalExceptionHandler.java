package com.mango.products.api;

import com.mango.products.service.PriceNotFoundException;
import com.mango.products.service.PriceOverlapException;
import com.mango.products.service.ProductNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Uniform RFC 7807 (Problem Details) error responses for all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PriceNotFoundException.class)
    public ProblemDetail handlePriceNotFound(PriceNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PriceOverlapException.class)
    public ProblemDetail handlePriceOverlap(PriceOverlapException ex) {
        return problemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail(HttpStatus.BAD_REQUEST, message));
    }

    private ProblemDetail problemDetail(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
