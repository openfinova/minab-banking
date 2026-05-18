package com.openfinova.banking.exchangerate.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.config.ExchangeRateProperties;
import com.openfinova.banking.exchangerate.entity.ExchangeRate;
import com.openfinova.banking.exchangerate.provider.ExchangeRateProvider;
import com.openfinova.banking.exchangerate.provider.ExchangeRateProviderException;
import com.openfinova.banking.exchangerate.repository.ExchangeRateRepository;
import com.openfinova.banking.setup.api.DateTimeService;

@ExtendWith(MockitoExtension.class)
class ExchangeRateSyncServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 3, 15);

    @Mock
    private ExchangeRateRepository repository;
    @Mock
    private ExchangeRateProvider provider;
    @Mock
    private DateTimeService dateTimeService;

    private ExchangeRateProperties properties;

    @InjectMocks
    private ExchangeRateSyncService syncService;

    @BeforeEach
    void setUp() {
        properties = new ExchangeRateProperties();
        properties.setManagedCurrencies(new java.util.ArrayList<>(List.of("USD", "GBP", "CHF")));
        // Re-inject the freshly initialized properties since @InjectMocks builds the service
        // before BeforeEach can mutate the field
        ReflectionTestUtils.setField(syncService, "properties", properties);
        ReflectionTestUtils.setField(syncService, "baseCurrency", "EUR");
    }

    @Test
    void sync_disabled_returnsDisabledResultAndDoesNotCallProvider() {
        properties.getSync().setEnabled(false);

        ExchangeRateSyncService.SyncResult result = syncService.sync();

        assertThat(result.providerId()).isEqualTo("disabled");
        verify(provider, never()).fetchLatestRates(anyString(), anySet());
        verify(repository, never()).save(any());
    }

    @Test
    void sync_insertsMissingPairsSkipsExistingOnes() {
        when(dateTimeService.today()).thenReturn(TODAY);
        when(provider.fetchLatestRates(eq("EUR"), anySet())).thenReturn(
                new ExchangeRateProvider.ProviderRates(
                        "EUR",
                        TODAY,
                        Map.of(
                                "USD",
                                new BigDecimal("1.0858"),
                                "GBP",
                                new BigDecimal("0.85420"),
                                "CHF",
                                new BigDecimal("0.9620"))));
        when(
                repository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        "EUR",
                        "USD",
                        TODAY,
                        RateType.SPOT))
                .thenReturn(false);
        when(
                repository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        "EUR",
                        "GBP",
                        TODAY,
                        RateType.SPOT))
                .thenReturn(true);
        when(
                repository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        "EUR",
                        "CHF",
                        TODAY,
                        RateType.SPOT))
                .thenReturn(false);

        ExchangeRateSyncService.SyncResult result = syncService.sync();

        assertThat(result.inserted()).containsExactlyInAnyOrder("USD", "CHF");
        assertThat(result.skippedAlreadyPresent()).containsExactly("GBP");
        assertThat(result.unsupportedByProvider()).isEmpty();
        verify(repository, times(2)).save(any(ExchangeRate.class));
    }

    @Test
    void sync_recordsUnsupportedCurrenciesProviderDidNotReturn() {
        when(dateTimeService.today()).thenReturn(TODAY);
        when(provider.fetchLatestRates(eq("EUR"), anySet())).thenReturn(
                new ExchangeRateProvider.ProviderRates("EUR", TODAY, Map.of("USD", new BigDecimal("1.0858"))));
        when(
                repository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        eq("EUR"),
                        anyString(),
                        eq(TODAY),
                        eq(RateType.SPOT)))
                .thenReturn(false);

        ExchangeRateSyncService.SyncResult result = syncService.sync();

        assertThat(result.inserted()).containsExactly("USD");
        assertThat(result.unsupportedByProvider()).containsExactlyInAnyOrder("GBP", "CHF");
        verify(repository, times(1)).save(any(ExchangeRate.class));
    }

    @Test
    void sync_filtersOutBaseCurrencyAndUnsupportedCodesFromManagedList() {
        properties.setManagedCurrencies(new java.util.ArrayList<>(List.of("EUR", "USD", "XYZ")));
        when(dateTimeService.today()).thenReturn(TODAY);
        when(provider.fetchLatestRates(eq("EUR"), anySet())).thenReturn(
                new ExchangeRateProvider.ProviderRates("EUR", TODAY, Map.of("USD", new BigDecimal("1.0858"))));
        when(
                repository.existsBySourceCurrencyAndTargetCurrencyAndRateDateAndRateType(
                        "EUR",
                        "USD",
                        TODAY,
                        RateType.SPOT))
                .thenReturn(false);

        ExchangeRateSyncService.SyncResult result = syncService.sync();

        assertThat(result.inserted()).containsExactly("USD");
    }

    @Test
    void sync_propagatesProviderException() {
        when(dateTimeService.today()).thenReturn(TODAY);
        when(provider.fetchLatestRates(eq("EUR"), anySet()))
                .thenThrow(new ExchangeRateProviderException("upstream down"));

        assertThatThrownBy(() -> syncService.sync()).isInstanceOf(ExchangeRateProviderException.class);
        verify(repository, never()).save(any());
    }
}
