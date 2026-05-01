package com.openfinova.banking.tp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.openfinova.banking.tp.entity.CompensationWorkflow;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Repository
public class CompensationWorkflowRepositoryImpl implements CompensationWorkflowRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<CompensationWorkflow> findByCompensationStepId(String stepId) {
        if (stepId == null) {
            return Optional.empty();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CompensationWorkflow> cq = cb.createQuery(CompensationWorkflow.class);
        Root<CompensationWorkflow> root = cq.from(CompensationWorkflow.class);
        cq.select(root);
        List<CompensationWorkflow> workflows = entityManager.createQuery(cq).getResultList();
        return workflows.stream().filter(w -> w.getCompensationSteps() != null)
                .filter(w -> w.getCompensationSteps().stream().anyMatch(s -> stepId.equals(s.getStepId()))).findFirst();
    }
}
