package com.openfinova.banking.exchangerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.config.ExchangeRateProperties;
import com.openfinova.banking.exchangerate.entity.ExchangeRate;
import com.openfinova.banking.exchangerate.repository.ExchangeRateRepository;
import com.openfinova.banking.exchangerate.repository.FXSpreadRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Exercises the read-side staleness fallback added in {@code getExchangeRateInternal}: when an
 * exact-date lookup misses, the service returns the most recent prior rate within the configured
 * window (default 7 days).
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplStalenessTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 3, 18); // Mon
    private static final LocalDate FRIDAY = LocalDate.of(2024, 3, 15);

    @Mock
    private ExchangeRateRepository repository;
    @Mock
    private FXSpreadRepository fxSpreadRepository;
    @Mock
    private DateTimeService dateTimeService;

    private ExchangeRateServiceImpl service;

    @BeforeEach
    void setUp() {
        ExchangeRateProperties properties = new ExchangeRateProperties();
        properties.setMaxStalenessDays(7);
        service = new ExchangeRateServiceImpl(repository, dateTimeService, fxSpreadRepository, properties);
        ReflectionTestUtils.setField(service, "baseCurrency", "EUR");
        lenient().when(dateTimeService.today()).thenReturn(TODAY);
    }

    @Test
    void getExchangeRate_fallsBackToMostRecentWhenExactDateMisses() {
        ExchangeRate friday = rate("EUR", "USD", new BigDecimal("1.0858"), FRIDAY);

        when(repository.findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType("EUR", "USD", TODAY, RateType.SPOT))
                .thenReturn(Optional.empty());
        when(repository.findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType("USD", "EUR", TODAY, RateType.SPOT))
                .thenReturn(Optional.empty());
        when(
                repository.findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
                        eq("EUR"),
                        eq("USD"),
                        eq(RateType.SPOT),
                        any(LocalDate.class),
                        eq(TODAY)))
                .thenReturn(Optional.of(friday));

        BigDecimal result = service.getExchangeRate("EUR", "USD", TODAY);

        assertThat(result).isEqualByComparingTo("1.0858");
    }

    @Test
    void getExchangeRate_stalenessFallbackUsesInverseWhenDirectMissing() {
        ExchangeRate inverse = rate("USD", "EUR", new BigDecimal("0.92"), FRIDAY);

        when(repository.findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType("EUR", "USD", TODAY, RateType.SPOT))
                .thenReturn(Optional.empty());
        when(repository.findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType("USD", "EUR", TODAY, RateType.SPOT))
                .thenReturn(Optional.empty());
        when(
                repository.findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
                        eq("EUR"),
                        eq("USD"),
                        eq(RateType.SPOT),
                        any(LocalDate.class),
                        eq(TODAY)))
                .thenReturn(Optional.empty());
        when(
                repository.findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
                        eq("USD"),
                        eq("EUR"),
                        eq(RateType.SPOT),
                        any(LocalDate.class),
                        eq(TODAY)))
                .thenReturn(Optional.of(inverse));

        BigDecimal result = service.getExchangeRate("EUR", "USD", TODAY);

        // 1 / 0.92 = ~1.0869... rounded to 8 decimals
        assertThat(result.compareTo(new BigDecimal("1.08695652"))).isZero();
    }

    @Test
    void getExchangeRate_failsWhenNothingFoundEvenWithStalenessWindow() {
        when(repository.findBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(
                repository.findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeAndRateDateBetweenOrderByRateDateDesc(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExchangeRate("EUR", "USD", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ExchangeRate rate(String source, String target, BigDecimal value, LocalDate date) {
        ExchangeRate r = new ExchangeRate(source, target, value, date, RateType.SPOT);
        return r;
    }
}
