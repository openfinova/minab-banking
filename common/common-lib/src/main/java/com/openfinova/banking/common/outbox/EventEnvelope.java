package com.openfinova.banking.common.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Canonical envelope for outbox relay / future broker migration. Kept JSON-only on the wire.
 */
public record EventEnvelope(UUID eventId, String type, Instant occurredAt, int schemaVersion, String payloadJson) {
}
