package com.ecommerce.payment.service.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment.kafka.dto.EventMetadata;
import com.ecommerce.payment.kafka.dto.SagaEventPayload;
import com.ecommerce.payment.model.db.entity.OutboxEvent;
import com.ecommerce.payment.model.db.entity.Payment;
import com.ecommerce.payment.model.db.entity.PaymentItem;
import com.ecommerce.payment.repository.db.PaymentRepository;
import com.ecommerce.payment.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSagaServiceImpl implements PaymentSagaService {

    private final PaymentRepository paymentRepository;
    private final JsonUtil jsonUtil;

    private static final String STATUS_PENDING = "PENDING";

    @Override
    @Transactional
    public void processStockReserved(OutboxEvent incomingEvent) {

        SagaEventPayload payload = jsonUtil.fromJson(incomingEvent.getPayload(), SagaEventPayload.class);
        EventMetadata metadata = jsonUtil.fromJson(incomingEvent.getMetadata(), EventMetadata.class);

        Payment payment = Payment.builder()
                .orderId(payload.orderId())
                .amount(payload.totalAmount())
                .status(STATUS_PENDING)
                .thirdPartyTxId(UUID.randomUUID().toString())
                .build();

        List<PaymentItem> paymentItems = payload.items().stream()
                .map(item -> PaymentItem.builder()
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .build())
                .toList();

        payment.addItems(paymentItems);

        try {
            paymentRepository.save(payment);

            log.info("[Saga] Successfully created PENDING payment record for OrderId: {}. ThirdPartyTxId: {}",
                    payload.orderId(), payment.getThirdPartyTxId());

        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "[Saga] Idempotency check failed (Duplicate). Payment record for OrderId: {} already exists. Ignoring event.",
                    payload.orderId());
        } catch (Exception e) {
            log.error("[Saga] Failed to create payment record for OrderId: {}. Error: {}",
                    payload.orderId(), e.getMessage());
            throw e;
        }
    }
}
