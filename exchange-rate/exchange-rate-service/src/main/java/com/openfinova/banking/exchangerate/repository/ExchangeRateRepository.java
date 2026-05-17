package com.openfinova.banking.exchangerate.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.entity.ExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    /**
     * Find exchange rate by currency pair, date, and type.
     *
     * @param sourceCurrency the source currency code
     * @param targetCurrency the target currency code
     * @param rateDate the rate date
     * @param rateType the rate type
     * @return optional containing the exchange rate if found
     */
    Optional<ExchangeRate> findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(String sourceCurrency,
            String targetCurrency, LocalDate rateDate, RateType rateType);

    /**
     * Find the latest exchange rate for a currency pair and type.
     *
     * @param sourceCurrency the source currency code
     * @param targetCurrency the target currency code
     * @param rateType the rate type
     * @return optional containing the latest exchange rate if found
     */
    Optional<ExchangeRate> findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeOrderByRateDateDesc(
            String sourceCurrency, String targetCurrency, RateType rateType);

    /**
     * Find exchange rates by currency pair within a date range.
     *
     * @param sourceCurrency the source currency code
     * @param targetCurrency the target currency code
     * @param startDate the start date
     * @param endDate the end date
     * @param rateType the rate type
     * @return list of exchange rates in the date range
     */
    List<ExchangeRate> findBySourceCurrencyAndTargetCurrencyAndRateDateBetweenAndRateType(String sourceCurrency,
            String targetCurrency, LocalDate startDate, LocalDate endDate, RateType rateType);

    /**
     * Find exchange rates by source currency and date range.
     *
     * @param sourceCurrency the source currency code
     * @param startDate the start date
     * @param endDate the end date
     * @param rateType the rate type
     * @param pageable pagination information
     * @return page of exchange rates
     */
    Page<ExchangeRate> findBySourceCurrencyAndRateDateBetweenAndRateType(String sourceCurrency, LocalDate startDate,
            LocalDate endDate, RateType rateType, Pageable pageable);

    /**
     * Find exchange rates by target currency and date range.
     *
     * @param targetCurrency the target currency code
     * @param startDate the start date
     * @param endDate the end date
     * @param rateType the rate type
     * @param pageable pagination information
     * @return page of exchange rates
     */
    Page<ExchangeRate> findByTargetCurrencyAndRateDateBetweenAndRateType(String targetCurrency, LocalDate startDate,
            LocalDate endDate, RateType rateType, Pageable pageable);

    /**
     * Find exchange rates by rate date and type.
     *
     * @param rateDate the rate date
     * @param rateType the rate type
     * @param pageable pagination information
     * @return page of exchange rates for the date
     */
    Page<ExchangeRate> findByRateDateAndRateType(LocalDate rateDate, RateType rateType, Pageable pageable);

    /**
     * Find all exchange rates for a specific currency (as source or target).
     *
     * @param currency the currency code
     * @param rateDate the rate date
     * @param rateType the rate type
     * @return list of exchange rates involving the currency
     */
    @Query("""
            SELECT er FROM ExchangeRate er
            WHERE (er.sourceCurrency = :currency OR er.targetCurrency = :currency)
            AND er.rateDate = :rateDate
            AND er.rateType = :rateType
            """)
    List<ExchangeRate> findByCurrencyAndRateDateAndRateType(@Param("currency") String currency,
            @Param("rateDate") LocalDate rateDate, @Param("rateType") RateType rateType);

    /**
     * Check if exchange rate exists for currency pair, date, and type.
     *
     * @param sourceCurrency the source currency code
     * @param targetCurrency the target currency code
     * @param rateDate the rate date
     * @param rateType the rate type
     * @return true if exchange rate exists
     */
    boolean existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(String sourceCurrency, String targetCurrency,
            LocalDate rateDate, RateType rateType);

    /**
     * Check if at least one exchange rate row exists for the given source currency. Used by the
     * startup data initializer to decide whether a seed sync is required.
     */
    boolean existsBySourceCurrency(String sourceCurrency);

    /**
     * Find the most recent exchange rate within a date window. Used by the read-side staleness
     * fallback when an exact-date lookup misses.
     *
     * @param sourceCurrency the source currency code
     * @param targetCurrency the target currency code
     * @param rateType the rate type
     * @param startDate inclusive lower bound (e.g. requested date minus N staleness days)
     * @param endDate inclusive upper bound (e.g. the requested date itself)
     */
    Optional<ExchangeRate> findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
            String sourceCurrency, String targetCurrency, RateType rateType, LocalDate startDate, LocalDate endDate);

    /**
     * Find the most recent exchange rates for all currency pairs.
     *
     * @param rateType the rate type
     * @param pageable pagination information
     * @return page of most recent exchange rates
     */
    @Query("""
            SELECT er FROM ExchangeRate er
            WHERE er.rateDate = (
                SELECT MAX(er2.rateDate) FROM ExchangeRate er2
                WHERE er2.sourceCurrency = er.sourceCurrency
                AND er2.targetCurrency = er.targetCurrency
                AND er2.rateType = :rateType
            )
            AND er.rateType = :rateType
            """)
    Page<ExchangeRate> findLatestRatesByType(@Param("rateType") RateType rateType, Pageable pageable);

    /**
     * Find exchange rates that need to be updated (older than specified days).
     *
     * @param cutoffDate the cutoff date for stale rates
     * @param rateType the rate type
     * @param pageable pagination information
     * @return page of stale exchange rates
     */
    @Query("""
            SELECT er FROM ExchangeRate er
            WHERE er.rateDate < :cutoffDate
            AND er.rateType = :rateType
            """)
    Page<ExchangeRate> findStaleRates(@Param("cutoffDate") LocalDate cutoffDate, @Param("rateType") RateType rateType,
            Pageable pageable);

    /**
     * Count exchange rates by rate type.
     *
     * @param rateType the rate type
     * @return count of exchange rates
     */
    long countByRateType(RateType rateType);

    /**
     * Count exchange rates for a specific date.
     *
     * @param rateDate the rate date
     * @return count of exchange rates for the date
     */
    long countByRateDate(LocalDate rateDate);

    /**
     * Find distinct source currencies.
     *
     * @return list of distinct source currencies
     */
    @Query("""
            SELECT DISTINCT er.sourceCurrency FROM ExchangeRate er
            ORDER BY er.sourceCurrency
            """)
    List<String> findDistinctSourceCurrencies();

    /**
     * Find distinct target currencies.
     *
     * @return list of distinct target currencies
     */
    @Query("""
            SELECT DISTINCT er.targetCurrency FROM ExchangeRate er
            ORDER BY er.targetCurrency
            """)
    List<String> findDistinctTargetCurrencies();

    /**
     * Find all distinct currencies (source and target combined).
     *
     * @return list of all distinct currencies
     */
    @Query("""
            SELECT DISTINCT currency FROM (
                SELECT er.sourceCurrency as currency FROM ExchangeRate er
                UNION
                SELECT er.targetCurrency as currency FROM ExchangeRate er
            )
            ORDER BY currency
            """)
    List<String> findAllDistinctCurrencies();
}
