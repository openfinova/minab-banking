package com.openfinova.banking.customer.outbox;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOutboxRepository extends JpaRepository<CustomerOutboxEvent, UUID> {
}
