package com.ecommerce.payment.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.ecommerce.payment.model.db.entity.OutboxEvent;
import com.ecommerce.payment.service.payment.PaymentSagaService;
import com.ecommerce.payment.util.JsonUtil;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final PaymentSagaService paymentSagaService;
    private final JsonUtil jsonUtil;
    private final Tracer tracer;

    private static final String EVENT_TYPE_STOCK_RESERVED = "STOCK_RESERVED";
    private static final String EVENT_TYPE_STOCK_RESERVE_FAILED = "STOCK_RESERVE_FAILED";

    @KafkaListener(topics = "stocks", groupId = "payment-service-group")
    public void handleStockEvent(String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        Span consumerSpan = tracer.spanBuilder("consume-stock-reserved").startSpan();

        try {
            OutboxEvent incomingEvent = jsonUtil.fromJson(message, OutboxEvent.class);
            String eventType = incomingEvent.getEventType();

            if (EVENT_TYPE_STOCK_RESERVED.equals(eventType)) {

                log.info("[Consumer] Received event: {}. Key: {}", eventType, key);

                consumerSpan.setAttribute("kafka.event.type", eventType);
                consumerSpan.setAttribute("kafka.event.id", incomingEvent.getEventId());
                consumerSpan.setAttribute("kafka.aggregate.id", incomingEvent.getAggregateId());

                try (Scope ws = consumerSpan.makeCurrent()) {
                    paymentSagaService.processStockReserved(incomingEvent);
                }

            } else if (EVENT_TYPE_STOCK_RESERVE_FAILED.equals(eventType)) {
                log.warn("[Consumer] Ignoring event: {} as it requires no action in PaymentService. Key: {}",
                        eventType, key);
            }

        } catch (Exception e) {
            log.error("[Consumer] Failed to process event from 'stocks' topic. Key: {}. Error: {}",
                    key, e.getMessage(), e);
            consumerSpan.setStatus(StatusCode.ERROR, e.getMessage());
            consumerSpan.recordException(e);
        }
    }
}
