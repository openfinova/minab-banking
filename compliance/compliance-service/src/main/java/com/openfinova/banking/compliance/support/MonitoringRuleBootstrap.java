package com.openfinova.banking.compliance.support;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.openfinova.banking.compliance.api.entity.AmlSeverity;
import com.openfinova.banking.compliance.entity.MonitoringRule;
import com.openfinova.banking.compliance.repository.MonitoringRuleRepository;

/**
 * Seeds default monitoring rules until a proper configuration UI exists.
 */
@Component
@Order(50)
public class MonitoringRuleBootstrap implements ApplicationRunner {

    private final MonitoringRuleRepository monitoringRuleRepository;

    public MonitoringRuleBootstrap(MonitoringRuleRepository monitoringRuleRepository) {
        this.monitoringRuleRepository = monitoringRuleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (monitoringRuleRepository.count() > 0) {
            return;
        }
        monitoringRuleRepository.save(structuringBandRule());
        monitoringRuleRepository.save(elevated());
        monitoringRuleRepository.save(criticalVolume());
    }

    private MonitoringRule structuringBandRule() {
        MonitoringRule rule = baseRule(
                "AML_STRUCTURING_BAND",
                "Structuring band watch (traditional cash thresholds — demo calibration)",
                10);
        rule.setThresholdMinInclusive(new BigDecimal("9000.00"));
        rule.setThresholdMaxInclusive(new BigDecimal("9999.99"));
        rule.setMatchTransactionTypes("ALL");
        rule.setSeverity(AmlSeverity.WARNING);
        rule.setInvestigationHoldRecommended(false);
        return rule;
    }

    private MonitoringRule elevated() {
        MonitoringRule rule = baseRule("AML_ELEVATED_TX", "Elevated outbound / transfer amount", 20);
        rule.setThresholdMinInclusive(new BigDecimal("15000.00"));
        rule.setThresholdMaxInclusive(null);
        rule.setMatchTransactionTypes("ALL");
        rule.setSeverity(AmlSeverity.HIGH);
        rule.setInvestigationHoldRecommended(false);
        return rule;
    }

    private MonitoringRule criticalVolume() {
        MonitoringRule rule = baseRule("AML_CRITICAL_VOLUME", "Critical value transaction", 30);
        rule.setThresholdMinInclusive(new BigDecimal("75000.00"));
        rule.setThresholdMaxInclusive(null);
        rule.setMatchTransactionTypes("ALL");
        rule.setSeverity(AmlSeverity.CRITICAL);
        rule.setInvestigationHoldRecommended(true);
        return rule;
    }

    private static MonitoringRule baseRule(String code, String displayName, int sortOrder) {
        MonitoringRule rule = new MonitoringRule();
        rule.setCode(code);
        rule.setDisplayName(displayName);
        rule.setSortOrder(sortOrder);
        rule.setEnabled(true);
        rule.setInvestigationHoldRecommended(false);
        return rule;
    }
}
