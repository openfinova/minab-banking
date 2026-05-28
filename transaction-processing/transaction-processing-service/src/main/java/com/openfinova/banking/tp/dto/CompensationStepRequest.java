package com.openfinova.banking.tp.dto;

import java.util.Map;

import com.openfinova.banking.tp.api.entity.CompensationStepType;

import jakarta.validation.constraints.NotNull;

public class CompensationStepRequest {

    private String stepId;

    @NotNull
    private CompensationStepType stepType;

    private String description;
    private Map<String, Object> parameters;
    private int order;

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
}
