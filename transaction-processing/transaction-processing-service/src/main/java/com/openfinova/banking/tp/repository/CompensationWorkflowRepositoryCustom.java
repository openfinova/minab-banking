package com.openfinova.banking.tp.repository;

import java.util.Optional;

import com.openfinova.banking.tp.entity.CompensationWorkflow;

/**
 * Custom queries for {@link CompensationWorkflow} that cannot be expressed in JPQL
 * (e.g. filtering inside JSON-converted {@code compensationSteps}).
 */
public interface CompensationWorkflowRepositoryCustom {

    /**
     * Finds the workflow that contains a compensation step with the given step id.
     * Steps are stored as JSON ({@link com.openfinova.banking.tp.converter.CompensationStepListConverter}),
     * so this scans loaded workflows in memory. For high volume, consider a native JSON query on PostgreSQL.
     */
    Optional<CompensationWorkflow> findByCompensationStepId(String stepId);
}
