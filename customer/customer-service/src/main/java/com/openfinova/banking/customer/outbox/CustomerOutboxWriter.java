package com.openfinova.banking.customer.outbox;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.outbox.OutboxWriter;

@Service
public class CustomerOutboxWriter implements OutboxWriter {

    private final CustomerOutboxRepository repository;

    public CustomerOutboxWriter(CustomerOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UUID append(String aggregateType, UUID aggregateId, String eventType, String payloadJson) {
        CustomerOutboxEvent row = new CustomerOutboxEvent();
        row.setAggregateType(aggregateType);
        row.setAggregateId(aggregateId);
        row.setEventType(eventType);
        row.setPayloadJson(payloadJson);
        return repository.save(row).getId();
    }
}
