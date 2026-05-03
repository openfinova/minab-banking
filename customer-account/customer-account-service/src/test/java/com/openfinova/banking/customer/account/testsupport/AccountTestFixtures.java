package com.openfinova.banking.customer.account.testsupport;

import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import com.openfinova.banking.customer.account.entity.Account;

public final class AccountTestFixtures {

    private AccountTestFixtures() {
    }

    /**
     * Minimal persisted-shaped account for entity unit tests (no validation ran).
     */
    public static Account checkingAccount() {
        Account a = new Account(UUID.randomUUID(), AccountProductType.CHECKING, "test");
        a.setAccountNumber("ACC12345678");
        a.setCurrency("USD");
        return a;
    }
}
