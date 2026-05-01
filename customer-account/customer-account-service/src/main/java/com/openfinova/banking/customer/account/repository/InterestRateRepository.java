package com.openfinova.banking.customer.account.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.customer.account.entity.InterestRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRateRepository extends JpaRepository<InterestRate, UUID> {

    @Query("""
            SELECT ir FROM InterestRate ir
            WHERE ir.customerAccount.id = :accountId
            AND ir.rateType = :rateType
            AND ir.effectiveFrom <= :asOfDate
            AND (ir.effectiveUntil IS NULL OR ir.effectiveUntil > :asOfDate)
            ORDER BY ir.effectiveFrom DESC
            """)
    Optional<InterestRate> findCurrentRateByAccountAndType(@Param("accountId") UUID accountId,
            @Param("rateType") InterestRate.RateType rateType, @Param("asOfDate") LocalDateTime asOfDate);

    @Query("""
            SELECT ir FROM InterestRate ir
            WHERE ir.customerAccount.id = :accountId
            AND ir.effectiveFrom <= :asOfDate
            AND (ir.effectiveUntil IS NULL OR ir.effectiveUntil > :asOfDate)
            ORDER BY ir.rateType, ir.effectiveFrom DESC
            """)
    List<InterestRate> findCurrentRatesByAccount(@Param("accountId") UUID accountId,
            @Param("asOfDate") LocalDateTime asOfDate);
}
