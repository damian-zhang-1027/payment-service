package com.ecommerce.payment.kafka.producer;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecommerce.payment.model.db.entity.OutboxEvent;
import com.ecommerce.payment.repository.db.OutboxEventRepository;
import com.ecommerce.payment.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonUtil jsonUtil;

    private static final Pageable BATCH_PAGEABLE = PageRequest.of(0, 100);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    // private static final String STATUS_FAILED = "FAILED";

    @Scheduled(fixedDelayString = "${kafka.poller.delay.ms:1000}")
    public void pollOutboxEvents() {

        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByUpdatedAtAsc(
                STATUS_PENDING,
                BATCH_PAGEABLE);

        if (events.isEmpty()) {
            return;
        }

        log.info("[Outbox] Found {} pending events to publish...", events.size());
        List<OutboxEvent> sentEvents = new ArrayList<>();

        for (OutboxEvent event : events) {
            try {
                String topic = event.getAggregateType();
                String key = event.getAggregateId();
                String value = jsonUtil.toJson(event);
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

                kafkaTemplate.send(record).get();

                event.setStatus(STATUS_SENT);
                sentEvents.add(event);

            } catch (Exception e) {
                // wait for next poll
                log.error("[Outbox] Failed to send eventId: {}. Error: {}", event.getEventId(), e.getMessage());
            }
        }

        if (!sentEvents.isEmpty()) {
            outboxEventRepository.saveAll(sentEvents);
            log.info("[Outbox] Successfully published {} events.", sentEvents.size());
        }
    }
}
