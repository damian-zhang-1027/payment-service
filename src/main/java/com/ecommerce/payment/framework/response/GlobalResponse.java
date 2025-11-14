package com.ecommerce.payment.framework.response;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard HTTP Response Wrapper, as per company specification.
 */
@Schema(description = "Standard API Response Format")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GlobalResponse<T>(
        @Schema(description = "Business status code (0=success, -1=failure)", example = "0") int retCode,
        @Schema(description = "Response payload") T data,
        @Schema(description = "Metadata (e.g., pagination, tracing ID)") Object meta,
        @Schema(description = "Error message (null on success)", example = "Product not found") String msg) {

    public static <T> GlobalResponse<T> success(T data) {
        return new GlobalResponse<>(0, data, null, null);
    }

    public static <T> GlobalResponse<T> success(T data, Object meta) {
        return new GlobalResponse<>(0, data, meta, null);
    }

    public static GlobalResponse<Object> error(String message) {
        return new GlobalResponse<>(-1, Collections.emptyMap(), null, message);
    }

    public static GlobalResponse<Object> error(String message, Object meta) {
        return new GlobalResponse<>(-1, Collections.emptyMap(), meta, message);
    }
}
