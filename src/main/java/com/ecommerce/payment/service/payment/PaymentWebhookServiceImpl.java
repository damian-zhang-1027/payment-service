package com.ecommerce.payment.service.payment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment.controller.payment.dto.InternalWebhookRequest;
import com.ecommerce.payment.controller.payment.dto.PaymentResponse;
import com.ecommerce.payment.exception.PaymentConflictException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.kafka.dto.EventMetadata;
import com.ecommerce.payment.kafka.dto.OrderItemDto;
import com.ecommerce.payment.kafka.dto.SagaEventPayload;
import com.ecommerce.payment.model.db.entity.OutboxEvent;
import com.ecommerce.payment.model.db.entity.Payment;
import com.ecommerce.payment.repository.db.OutboxEventRepository;
import com.ecommerce.payment.repository.db.PaymentRepository;
import com.ecommerce.payment.util.JsonUtil;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonUtil jsonUtil;
    private final Tracer tracer;

    private static final String TOPIC_PAYMENTS = "payments";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String EVENT_TYPE_PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    private static final String EVENT_TYPE_PAYMENT_FAILED = "PAYMENT_FAILED";

    @Override
    @Transactional
    public PaymentResponse processWebhook(InternalWebhookRequest request) {

        Long orderId = request.orderId();
        String newStatus = request.status().toUpperCase();

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment record not found for OrderId: " + orderId));

        if (!STATUS_PENDING.equals(payment.getStatus())) {
            throw new PaymentConflictException(
                    "Payment for OrderId: " + orderId + " is not in PENDING status. Current: " + payment.getStatus());
        }

        String newEventType;

        if (STATUS_SUCCEEDED.equals(newStatus)) {
            newEventType = EVENT_TYPE_PAYMENT_SUCCEEDED;
        } else {
            newEventType = EVENT_TYPE_PAYMENT_FAILED;
        }

        payment.setStatus(newStatus);
        Payment updatedPayment = paymentRepository.save(payment);

        List<OrderItemDto> itemsDto = payment.getItems().stream()
                .map(item -> new OrderItemDto(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        SagaEventPayload sagaPayload = new SagaEventPayload(
                payment.getOrderId(),
                payment.getAmount(),
                itemsDto);

        String traceId = getTraceIdFromCurrentSpan();

        EventMetadata metadata = EventMetadata.builder()
                .traceId(traceId)
                .causationId(traceId)
                .userId(null)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        OutboxEvent outgoingEvent = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType(TOPIC_PAYMENTS)
                .aggregateId(payment.getOrderId().toString())
                .eventType(newEventType)
                .payload(jsonUtil.toJson(sagaPayload))
                .metadata(jsonUtil.toJson(metadata))
                .status(STATUS_PENDING)
                .build();

        outboxEventRepository.save(outgoingEvent);

        log.info("[Saga] Webhook processed. OrderId: {} status updated to {}. Outbox event: {} created.",
                orderId, newStatus, newEventType);

        return PaymentResponse.fromEntity(updatedPayment);
    }

    private String getTraceIdFromCurrentSpan() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return "N/A_TRACE_ID";
    }
}
