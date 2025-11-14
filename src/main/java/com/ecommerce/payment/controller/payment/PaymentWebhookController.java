package com.ecommerce.payment.controller.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.payment.controller.payment.dto.InternalWebhookRequest;
import com.ecommerce.payment.controller.payment.dto.PaymentResponse;
import com.ecommerce.payment.service.payment.PaymentWebhookService;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;
    private final Tracer tracer;

    @PostMapping("/webhook/internal")
    public ResponseEntity<PaymentResponse> handleInternalWebhook(
            @Valid @RequestBody InternalWebhookRequest request) {

        Span httpSpan = tracer.spanBuilder("http-post-webhook-internal").startSpan();

        try (Scope ws = httpSpan.makeCurrent()) {
            log.info("Received internal webhook request for OrderId: {}, Status: {}",
                    request.orderId(), request.status());

            PaymentResponse response = paymentWebhookService.processWebhook(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing webhook for OrderId: {}. Error: {}",
                    request.orderId(), e.getMessage());
            httpSpan.recordException(e);
            throw e;
        } finally {
            httpSpan.end();
        }
    }
}
