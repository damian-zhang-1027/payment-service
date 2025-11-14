package com.ecommerce.payment.controller.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InternalWebhookRequest(
        @NotNull Long orderId,
        @NotBlank @Pattern(regexp = "SUCCEEDED|FAILED", message = "Status must be SUCCEEDED or FAILED") String status) {
}
