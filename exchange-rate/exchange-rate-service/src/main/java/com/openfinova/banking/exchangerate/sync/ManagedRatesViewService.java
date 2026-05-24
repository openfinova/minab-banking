package com.openfinova.banking.exchangerate.sync;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.entity.ExchangeRate;
import com.openfinova.banking.exchangerate.repository.ExchangeRateRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Read-only "today's rates" view powering the admin dashboard board: one row per managed currency,
 * showing the latest mid rate and metadata so the admin can decide whether to override it.
 *
 * <p>The view does not invent rows where none exist; if a pair has never been published, the row's
 * id/rate fields are {@code null} so the dashboard can render an "Add" affordance.
 */
@Service
public class ManagedRatesViewService {

    private final ExchangeRateRepository repository;
    private final ExchangeRateSyncService syncService;
    private final DateTimeService dateTimeService;

    public ManagedRatesViewService(ExchangeRateRepository repository, ExchangeRateSyncService syncService,
            DateTimeService dateTimeService) {
        this.repository = repository;
        this.syncService = syncService;
        this.dateTimeService = dateTimeService;
    }

    @Transactional(readOnly = true)
    public ManagedRatesView getView() {
        LocalDate today = dateTimeService.today();
        String base = syncService.getBaseCurrency();
        Set<String> targets = new TreeSet<>(syncService.resolveTargets());

        List<ManagedRateRow> rows = new ArrayList<>(targets.size());
        for (String target : targets) {
            Optional<ExchangeRate> latest = repository
                    .findFirstBySourceCurrencyAndTargetCurrencyAndRateTypeOrderByRateDateDesc(
                            base,
                            target,
                            RateType.SPOT);
            if (latest.isPresent()) {
                ExchangeRate r = latest.get();
                rows.add(
                        new ManagedRateRow(
                                r.getId(),
                                target,
                                r.getRate(),
                                r.getRateDate(),
                                !r.getRateDate().equals(today),
                                r.getCreatedBy(),
                                r.getUpdatedBy(),
                                r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt()));
            } else {
                rows.add(ManagedRateRow.missing(target));
            }
        }

        return new ManagedRatesView(base, today, rows);
    }

    public record ManagedRatesView(String baseCurrency, LocalDate today, List<ManagedRateRow> rows) {
    }

    public record ManagedRateRow(UUID id, String targetCurrency, BigDecimal rate, LocalDate rateDate, boolean stale,
            String createdBy, String updatedBy, LocalDateTime lastChangedAt) {

        public static ManagedRateRow missing(String targetCurrency) {
            return new ManagedRateRow(null, targetCurrency, null, null, false, null, null, null);
        }
    }
}
