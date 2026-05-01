package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.entity.GLTransactionSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for GLTransactionSequence entity.
 * Provides data access methods with pessimistic locking for gapless sequence generation.
 */
public interface GLTransactionSequenceRepository extends JpaRepository<GLTransactionSequence, UUID> {

    /**
     * Finds the sequence for a fiscal period with pessimistic write lock.
     * 
     * This method acquires a database-level lock on the sequence row to prevent
     * concurrent transaction number assignment. The lock is held until the
     * transaction commits or rolls back.
     * 
     * Lock behavior:
     * - Blocks other threads attempting to acquire the same sequence
     * - Prevents lost updates and race conditions
     * - Automatically released on transaction completion
     * 
     * Performance considerations:
     * - Lock duration should be minimal (only during number assignment)
     * - Per-period sequences reduce contention across periods
     * - Typical lock hold time: 5-10ms
     * 
     * @param fiscalPeriodId the UUID of the fiscal period
     * @return Optional containing the locked sequence, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM GLTransactionSequence s WHERE s.fiscalPeriod.id = :fiscalPeriodId")
    Optional<GLTransactionSequence> findByFiscalPeriodIdWithLock(@Param("fiscalPeriodId") UUID fiscalPeriodId);

    /**
     * Finds the sequence for a fiscal period without locking.
     * Use this for read-only operations like validation and reporting.
     * 
     * @param fiscalPeriodId the UUID of the fiscal period
     * @return Optional containing the sequence, or empty if not found
     */
    @Query("SELECT s FROM GLTransactionSequence s WHERE s.fiscalPeriod.id = :fiscalPeriodId")
    Optional<GLTransactionSequence> findByFiscalPeriodId(@Param("fiscalPeriodId") UUID fiscalPeriodId);

    /**
     * Checks if a sequence exists for a fiscal period.
     * 
     * @param fiscalPeriodId the UUID of the fiscal period
     * @return true if a sequence exists for this period
     */
    @Query("SELECT COUNT(s) > 0 FROM GLTransactionSequence s WHERE s.fiscalPeriod.id = :fiscalPeriodId")
    boolean existsByFiscalPeriodId(@Param("fiscalPeriodId") UUID fiscalPeriodId);
}
