package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.CompensationWorkflowResponse;
import com.openfinova.banking.tp.entity.CompensationWorkflow;
import org.springframework.stereotype.Component;

@Component
public class CompensationWorkflowMapper {

    public CompensationWorkflowResponse toResponse(CompensationWorkflow workflow) {
        if (workflow == null) {
            return null;
        }

        CompensationWorkflowResponse response = new CompensationWorkflowResponse();
        response.setId(workflow.getId());
        response.setTransactionId(
                workflow.getOriginalTransaction() != null ? workflow.getOriginalTransaction().getId() : null);
        response.setStatus(workflow.getWorkflowStatus());

        // Calculate step counts from compensationSteps list
        if (workflow.getCompensationSteps() != null) {
            response.setTotalSteps(workflow.getCompensationSteps().size());
            response.setCompletedSteps(
                    (int) workflow.getCompensationSteps().stream().filter(step -> step.getStatus().isSuccessful())
                            .count());
            response.setFailedSteps(
                    (int) workflow.getCompensationSteps().stream().filter(step -> step.getStatus().isFailed()).count());
        } else {
            response.setTotalSteps(0);
            response.setCompletedSteps(0);
            response.setFailedSteps(0);
        }

        response.setCreatedAt(workflow.getCreatedAt());
        response.setCompletedAt(workflow.getCompletedAt());

        return response;
    }
}
