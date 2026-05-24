package com.openfinova.banking.customer.outbox;

import com.openfinova.banking.common.outbox.OutboxRecord;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_outbox")
public class CustomerOutboxEvent extends OutboxRecord {
}
