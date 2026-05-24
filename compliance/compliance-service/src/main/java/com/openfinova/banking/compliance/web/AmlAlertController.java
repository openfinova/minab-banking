package com.openfinova.banking.compliance.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.compliance.api.dto.AmlAlertResponse;
import com.openfinova.banking.compliance.entity.AmlAlert;
import com.openfinova.banking.compliance.repository.AmlAlertRepository;

@RestController
@RequestMapping("/api/v1/aml/alerts")
public class AmlAlertController {

    private final AmlAlertRepository amlAlertRepository;

    public AmlAlertController(AmlAlertRepository amlAlertRepository) {
        this.amlAlertRepository = amlAlertRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('compliance:alert:read')")
    public Page<AmlAlertResponse> list(Pageable pageable) {
        return amlAlertRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    private AmlAlertResponse toResponse(AmlAlert a) {
        AmlAlertResponse r = new AmlAlertResponse();
        r.setId(a.getId());
        r.setTransactionId(a.getTransactionId());
        r.setAccountId(a.getSourceAccountId());
        r.setCustomerId(a.getCustomerPartyId());
        r.setRuleCode(a.getRuleCode());
        r.setSeverity(a.getSeverity());
        r.setStatus(a.getStatus());
        r.setAmount(a.getMonitoredAmount());
        r.setCurrency(a.getCurrency());
        r.setDetail(a.getDetailSummary());
        r.setInvestigationHoldPlaced(a.isInvestigationHoldPlaced());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
