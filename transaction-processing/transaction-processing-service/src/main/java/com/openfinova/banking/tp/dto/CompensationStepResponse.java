package com.openfinova.banking.tp.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.openfinova.banking.tp.api.entity.CompensationStepStatus;
import com.openfinova.banking.tp.api.entity.CompensationStepType;

public class CompensationStepResponse {

    private String stepId;
    private CompensationStepType stepType;
    private String description;
    private Map<String, Object> parameters;
    private int order;
    private CompensationStepStatus status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int retryCount;
    private Map<String, Object> result;

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public CompensationStepType getStepType() {
        return stepType;
    }

    public void setStepType(CompensationStepType stepType) {
        this.stepType = stepType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public CompensationStepStatus getStatus() {
        return status;
    }

    public void setStatus(CompensationStepStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
}
