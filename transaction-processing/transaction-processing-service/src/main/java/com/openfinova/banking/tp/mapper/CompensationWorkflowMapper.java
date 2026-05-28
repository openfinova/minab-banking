package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.CompensationWorkflowResponse;
import com.openfinova.banking.tp.dto.CompensationStepRequest;
import com.openfinova.banking.tp.dto.CompensationStepResponse;
import com.openfinova.banking.tp.entity.CompensationStep;
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

    public CompensationStepResponse toStepResponse(CompensationStep step) {
        if (step == null) {
            return null;
        }
        CompensationStepResponse response = new CompensationStepResponse();
        response.setStepId(step.getStepId());
        response.setStepType(step.getStepType());
        response.setDescription(step.getDescription());
        response.setParameters(step.getParameters());
        response.setOrder(step.getOrder());
        response.setStatus(step.getStatus());
        response.setErrorMessage(step.getErrorMessage());
        response.setStartedAt(step.getStartedAt());
        response.setCompletedAt(step.getCompletedAt());
        response.setRetryCount(step.getRetryCount());
        response.setResult(step.getResult());
        return response;
    }

    public java.util.List<CompensationStepResponse> toStepResponseList(java.util.List<CompensationStep> steps) {
        if (steps == null) {
            return java.util.List.of();
        }
        return steps.stream().map(this::toStepResponse).toList();
    }

    public CompensationStep toStepEntity(CompensationStepRequest request) {
        if (request == null) {
            return null;
        }
        CompensationStep step = new CompensationStep();
        step.setStepId(request.getStepId());
        step.setStepType(request.getStepType());
        step.setDescription(request.getDescription());
        step.setParameters(request.getParameters());
        step.setOrder(request.getOrder());
        return step;
    }

    public java.util.List<CompensationStep> toStepEntityList(java.util.List<CompensationStepRequest> requests) {
        if (requests == null) {
            return java.util.List.of();
        }
        return requests.stream().map(this::toStepEntity).toList();
    }
}
