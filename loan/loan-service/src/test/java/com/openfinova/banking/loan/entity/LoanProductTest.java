package com.openfinova.banking.loan.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class LoanProductTest {

    @Test
    void activateAndDeactivate_toggleActiveFlag() {
        LoanProduct p = new LoanProduct();
        p.setActive(false);
        assertThat(p.isActive()).isFalse();

        p.activate();
        assertThat(p.isActive()).isTrue();

        p.deactivate();
        assertThat(p.isActive()).isFalse();
    }
}
