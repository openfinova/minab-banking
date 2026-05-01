package com.openfinova.banking.gl.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.gl.api.entity.ReconciliationStatus;
import com.openfinova.banking.gl.entity.GLReconciliation;

/**
 * Repository for GL reconciliation records. Used to enforce that reconciled
 * transactions cannot be reversed.
 */
public interface GLReconciliationRepository extends JpaRepository<GLReconciliation, UUID> {

    /**
     * Find reconciliation record for a transaction with the given status.
     * Used to determine if a transaction is reconciled (status RECONCILED).
     */
    @Query("SELECT r FROM GLReconciliation r WHERE r.transaction.id = :transactionId AND r.status = :status")
    Optional<GLReconciliation> findByTransactionIdAndStatus(@Param("transactionId") UUID transactionId,
            @Param("status") ReconciliationStatus status);
}
