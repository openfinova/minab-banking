package com.openfinova.banking.loan.testsupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.AmortizationType;
import com.openfinova.banking.loan.api.entity.InterestCalculationMethod;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.api.entity.RepaymentFrequency;
import com.openfinova.banking.loan.entity.LoanAccount;

public final class LoanTestFixtures {

    private LoanTestFixtures() {
    }

    public static LoanAccount activeLoanAccount() {
        UUID applicationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LoanAccount account = new LoanAccount(
                applicationId,
                customerId,
                productId,
                new BigDecimal("10000.0000"),
                "USD");
        LocalDate disb = LocalDate.of(2025, 1, 1);
        account.setLoanAccountNumber("LN-TEST-" + applicationId.toString().substring(0, 8));
        account.setTenorMonths(12);
        account.setInterestRate(new BigDecimal("5.50"));
        account.setInterestCalculationMethod(InterestCalculationMethod.REDUCING_BALANCE);
        account.setRepaymentFrequency(RepaymentFrequency.MONTHLY);
        account.setAmortizationType(AmortizationType.EQUAL_INSTALLMENTS);
        account.setDisbursementDate(disb);
        account.setMaturityDate(disb.plusYears(1));
        account.setOutstandingPrincipal(new BigDecimal("8000.0000"));
        account.setOutstandingInterest(new BigDecimal("10.0000"));
        account.setOutstandingFees(new BigDecimal("2.5000"));
        account.setOutstandingPenalties(BigDecimal.ZERO);
        account.setStatus(LoanStatus.ACTIVE);
        account.setDaysPastDue(0);
        return account;
    }
}
