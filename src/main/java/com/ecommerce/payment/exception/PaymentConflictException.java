package com.ecommerce.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 用於 409 (如果支付狀態不是 PENDING)
@ResponseStatus(HttpStatus.CONFLICT)
public class PaymentConflictException extends RuntimeException {
    public PaymentConflictException(String message) {
        super(message);
    }
}
