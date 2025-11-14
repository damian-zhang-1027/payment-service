package com.ecommerce.payment.service.payment;

import com.ecommerce.payment.controller.payment.dto.InternalWebhookRequest;
import com.ecommerce.payment.controller.payment.dto.PaymentResponse;

public interface PaymentWebhookService {

    PaymentResponse processWebhook(InternalWebhookRequest request);
}
