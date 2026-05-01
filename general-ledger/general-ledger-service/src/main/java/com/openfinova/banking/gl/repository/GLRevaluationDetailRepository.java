package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.entity.GLRevaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for GLRevaluationDetail entity.
 * Provides data access methods for detailed revaluation information.
 */
public interface GLRevaluationDetailRepository extends JpaRepository<GLRevaluationDetail, UUID> {

    /**
     * Find all details for a specific revaluation run.
     */
    @Query("SELECT d FROM GLRevaluationDetail d WHERE d.revaluationRun.id = :revaluationRunId ORDER BY d.glAccount.code")
    List<GLRevaluationDetail> findByRevaluationRunId(@Param("revaluationRunId") UUID revaluationRunId);

    /**
     * Find all revaluation details for a specific account.
     */
    @Query("SELECT d FROM GLRevaluationDetail d WHERE d.glAccount.id = :accountId ORDER BY d.revaluationRun.revaluationDate DESC")
    List<GLRevaluationDetail> findByAccountId(@Param("accountId") UUID accountId);

    /**
     * Find revaluation details for a specific account and run.
     */
    @Query("SELECT d FROM GLRevaluationDetail d WHERE d.glAccount.id = :accountId AND d.revaluationRun.id = :revaluationRunId")
    List<GLRevaluationDetail> findByAccountIdAndRevaluationRunId(@Param("accountId") UUID accountId,
            @Param("revaluationRunId") UUID revaluationRunId);

    /**
     * Find details linked to a specific journal transaction.
     */
    @Query("SELECT d FROM GLRevaluationDetail d WHERE d.journalTransaction.id = :transactionId")
    List<GLRevaluationDetail> findByJournalTransactionId(@Param("transactionId") UUID transactionId);
}
