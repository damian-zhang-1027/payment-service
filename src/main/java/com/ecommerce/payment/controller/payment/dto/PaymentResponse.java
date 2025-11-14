package com.ecommerce.payment.controller.payment.dto;

import com.ecommerce.payment.model.db.entity.Payment;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        String status,
        Long amount,
        String thirdPartyTxId) {
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getThirdPartyTxId());
    }
}
