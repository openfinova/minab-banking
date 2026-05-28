package com.openfinova.banking.compliance.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.compliance.api.entity.RiskRating;
import com.openfinova.banking.compliance.api.event.CustomerRiskEvaluatedEvent;
import com.openfinova.banking.compliance.entity.CustomerRiskProfile;
import com.openfinova.banking.compliance.repository.CustomerRiskProfileRepository;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.customer.api.dto.CustomerInfo;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Orchestrates AML risk evaluation: screening, scoring, persistence, and downstream event publication.
 */
@Service
@Transactional
public class ComplianceEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceEvaluationService.class);

    private final CustomerInfoService customerInfoService;
    private final ScreeningService screeningService;
    private final RiskEngine riskEngine;
    private final CustomerRiskProfileRepository riskProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DateTimeService dateTimeService;

    public ComplianceEvaluationService(CustomerInfoService customerInfoService, ScreeningService screeningService,
            RiskEngine riskEngine, CustomerRiskProfileRepository riskProfileRepository,
            ApplicationEventPublisher eventPublisher, DateTimeService dateTimeService) {
        this.customerInfoService = customerInfoService;
        this.screeningService = screeningService;
        this.riskEngine = riskEngine;
        this.riskProfileRepository = riskProfileRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeService = dateTimeService;
    }

    public RiskRating evaluateCustomerRisk(UUID customerId) {
        logger.info("Evaluating AML risk for customer {}", customerId);

        CustomerInfo customer = customerInfoService.getCustomer(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        String name = customer.getFirstName() + " " + customer.getLastName();
        String country = customer.getResidenceCountry();

        boolean sanctionMatch = screeningService.checkSanctions(name, country);
        boolean pepMatch = screeningService.checkPep(name, country);

        RiskRating riskRating = riskEngine.calculateRisk(customer, pepMatch, sanctionMatch);

        CustomerRiskProfile profile = riskProfileRepository.findById(customerId).orElse(new CustomerRiskProfile());
        profile.setCustomerId(customerId);
        profile.setRiskRating(riskRating);
        profile.setPepMatch(pepMatch);
        profile.setSanctionMatch(sanctionMatch);
        profile.setLastEvaluatedAt(dateTimeService.now());

        riskProfileRepository.save(profile);

        eventPublisher.publishEvent(new CustomerRiskEvaluatedEvent(customerId, riskRating, pepMatch, sanctionMatch));

        return riskRating;
    }
}
