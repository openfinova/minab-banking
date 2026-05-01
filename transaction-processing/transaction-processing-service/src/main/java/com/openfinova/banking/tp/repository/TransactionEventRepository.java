package com.openfinova.banking.tp.repository;

import com.openfinova.banking.tp.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for TransactionEvent entities.
 */
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, UUID> {

    /**
     * Find all events for a transaction ordered by creation time.
     * 
     * @param transactionId the UUID of the transaction
     * @return list of transaction events
     */
    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);
}
