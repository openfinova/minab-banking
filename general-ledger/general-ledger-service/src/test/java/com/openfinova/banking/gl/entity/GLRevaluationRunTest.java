package com.openfinova.banking.gl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GLRevaluationRunTest {

    @Test
    void incrementsAndAccumulateAdjustment() {
        GLRevaluationRun run = new GLRevaluationRun(LocalDate.of(2026, 5, 1), "batch", "USD", "SCHEDULED");

        run.incrementAccountsProcessed();
        run.incrementAccountsRevalued();
        run.incrementAccountsFailed();
        assertThat(run.getAccountsProcessed()).isEqualTo(1);
        assertThat(run.getAccountsRevalued()).isEqualTo(1);
        assertThat(run.getAccountsFailed()).isEqualTo(1);

        run.addToTotalAdjustment(new BigDecimal("2.50"));
        assertThat(run.getTotalAdjustment()).isEqualByComparingTo("2.50");
    }
}
