package com.openfinova.banking.customer.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.customer.account.entity.GLAccountMapping;
import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GLAccountMappingRepository extends JpaRepository<GLAccountMapping, UUID> {

    /**
     * Finds all active GL account mappings for a given customer account.
     *
     * @param customerAccountId the customer account ID
     * @return list of active mappings
     */
    List<GLAccountMapping> findByCustomerAccountIdAndIsActiveTrue(UUID customerAccountId);

    /**
     * Finds an active GL account mapping for a specific account and mapping type.
     *
     * @param customerAccountId the customer account ID
     * @param mappingType the mapping type
     * @return optional containing the mapping if found
     */
    Optional<GLAccountMapping> findByCustomerAccountIdAndMappingTypeAndIsActiveTrue(UUID customerAccountId,
            GLAccountMappingType mappingType);
}
