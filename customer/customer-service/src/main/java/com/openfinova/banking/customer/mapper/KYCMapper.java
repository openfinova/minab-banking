package com.openfinova.banking.customer.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.openfinova.banking.customer.dto.KYCReviewStepResponse;
import com.openfinova.banking.customer.dto.KYCWorkflowResponse;
import com.openfinova.banking.customer.entity.KYCReviewStep;
import com.openfinova.banking.customer.entity.KYCWorkflow;

/** Maps KYC entities to JSON-safe DTOs (no JPA bidirectional cycles). */
public final class KYCMapper {

    private KYCMapper() {
    }

    public static KYCWorkflowResponse toWorkflowResponse(KYCWorkflow w) {
        if (w == null) {
            return null;
        }
        KYCWorkflowResponse dto = new KYCWorkflowResponse();
        dto.setId(w.getId());
        dto.setCustomerId(w.getCustomer() != null ? w.getCustomer().getId() : null);
        dto.setStatus(w.getStatus());
        dto.setInitiatedBy(w.getInitiatedBy());
        dto.setInitiatedAt(w.getInitiatedAt());
        dto.setCompletedAt(w.getCompletedAt());
        dto.setReviewedBy(w.getReviewedBy());
        dto.setReviewedAt(w.getReviewedAt());
        dto.setComments(w.getComments());
        dto.setRejectionReason(w.getRejectionReason());
        dto.setReVerificationReason(w.getReVerificationReason());
        dto.setVersion(w.getVersion());
        dto.setUpdatedAt(w.getUpdatedAt());
        if (w.getReviewSteps() != null) {
            dto.setReviewSteps(
                    w.getReviewSteps().stream().map(KYCMapper::toReviewStepResponse).collect(Collectors.toList()));
        }
        return dto;
    }

    public static KYCReviewStepResponse toReviewStepResponse(KYCReviewStep s) {
        if (s == null) {
            return null;
        }
        KYCReviewStepResponse dto = new KYCReviewStepResponse();
        dto.setId(s.getId());
        dto.setStepName(s.getStepName());
        dto.setDecision(s.getDecision());
        dto.setComments(s.getComments());
        dto.setReviewedBy(s.getReviewedBy());
        dto.setReviewedAt(s.getReviewedAt());
        return dto;
    }

    public static List<KYCWorkflowResponse> toWorkflowResponseList(List<KYCWorkflow> workflows) {
        if (workflows == null) {
            return List.of();
        }
        return workflows.stream().map(KYCMapper::toWorkflowResponse).collect(Collectors.toList());
    }
}
