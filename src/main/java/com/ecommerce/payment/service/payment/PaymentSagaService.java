package com.ecommerce.payment.service.payment;

import com.ecommerce.payment.model.db.entity.OutboxEvent;

public interface PaymentSagaService {

    void processStockReserved(OutboxEvent incomingEvent);
}
