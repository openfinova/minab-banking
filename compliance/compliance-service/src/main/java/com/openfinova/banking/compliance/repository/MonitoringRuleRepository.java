package com.openfinova.banking.compliance.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.compliance.entity.MonitoringRule;

public interface MonitoringRuleRepository extends JpaRepository<MonitoringRule, UUID> {

    List<MonitoringRule> findAllByEnabledTrueOrderBySortOrderAsc();
}
