package com.openfinova.banking.common.outbox;

import java.util.UUID;

/**
 * Producer-side API: append outbox rows inside the same transaction as the business mutation.
 */
public interface OutboxWriter {

    UUID append(String aggregateType, UUID aggregateId, String eventType, String payloadJson);
}
