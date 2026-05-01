package com.openfinova.banking.exchangerate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.exchangerate.entity.FXSpread;

/**
 * Repository for configurable FX spreads by currency pair.
 */
public interface FXSpreadRepository extends JpaRepository<FXSpread, UUID> {

    Optional<FXSpread> findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency);
}
