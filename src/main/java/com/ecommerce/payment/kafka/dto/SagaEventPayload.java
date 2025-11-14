package com.ecommerce.payment.kafka.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SagaEventPayload(
        @NotNull Long orderId,
        @NotNull @Positive Long totalAmount,
        @NotNull @Valid List<OrderItemDto> items) {
}
