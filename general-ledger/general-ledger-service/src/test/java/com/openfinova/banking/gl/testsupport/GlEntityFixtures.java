package com.openfinova.banking.gl.testsupport;

import java.time.LocalDate;

import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLTransaction;

public final class GlEntityFixtures {

    private GlEntityFixtures() {
    }

    public static GLAccount usdAssetAccount() {
        return new GLAccount("1000", "Cash", GLAccountType.ASSET, "USD");
    }

    public static GLAccount usdLiabilityAccount() {
        return new GLAccount("2000", "Deposits", GLAccountType.LIABILITY, "USD");
    }

    public static GLTransaction draftTransaction(String referenceId) {
        return new GLTransaction(referenceId, "Test", LocalDate.of(2026, 5, 1));
    }
}
