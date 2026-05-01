package com.openfinova.banking.tp.repository;

import com.openfinova.banking.tp.entity.TransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TransactionRequest entities.
 */
public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, UUID> {

    /**
     * Find a transaction request by its idempotency key.
     *
     * @param idempotencyKey the unique key
     * @return an optional containing the request if found
     */
    Optional<TransactionRequest> findByIdempotencyKey(String idempotencyKey);
}
