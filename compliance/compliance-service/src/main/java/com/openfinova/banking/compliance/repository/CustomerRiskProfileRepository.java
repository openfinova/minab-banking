package com.openfinova.banking.compliance.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openfinova.banking.compliance.entity.CustomerRiskProfile;

@Repository
public interface CustomerRiskProfileRepository extends JpaRepository<CustomerRiskProfile, UUID> {
}
