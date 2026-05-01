package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.entity.GLRevaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for GLRevaluationRun entity.
 * Provides data access methods for revaluation run audit trail.
 */
public interface GLRevaluationRunRepository extends JpaRepository<GLRevaluationRun, UUID> {

    /**
     * Find all revaluation runs for a specific date.
     */
    List<GLRevaluationRun> findByRevaluationDate(LocalDate revaluationDate);

    /**
     * Find the most recent revaluation run for a specific date.
     */
    @Query("SELECT r FROM GLRevaluationRun r WHERE r.revaluationDate = :revaluationDate ORDER BY r.executedAt DESC")
    Optional<GLRevaluationRun> findLatestByRevaluationDate(@Param("revaluationDate") LocalDate revaluationDate);

    /**
     * Find all revaluation runs within a date range.
     */
    @Query("SELECT r FROM GLRevaluationRun r WHERE r.revaluationDate BETWEEN :startDate AND :endDate ORDER BY r.revaluationDate DESC, r.executedAt DESC")
    List<GLRevaluationRun> findByRevaluationDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find revaluation runs by trigger type.
     */
    List<GLRevaluationRun> findByTriggerTypeOrderByExecutedAtDesc(String triggerType);

    /**
     * Find revaluation run by correlation ID (for period close tracking).
     */
    Optional<GLRevaluationRun> findByCorrelationId(UUID correlationId);

    /**
     * Find all revaluation runs ordered by execution time.
     */
    @Query("SELECT r FROM GLRevaluationRun r ORDER BY r.executedAt DESC")
    List<GLRevaluationRun> findAllOrderByExecutedAtDesc();
}
