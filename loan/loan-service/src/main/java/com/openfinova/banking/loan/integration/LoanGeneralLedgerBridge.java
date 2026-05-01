package com.openfinova.banking.loan.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.openfinova.banking.gl.api.GeneralLedgerService;
import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;

/**
 * Best-effort GL posting for loan interest accrual. Skips when GL is unavailable or accounts are not configured.
 */
@Component
public class LoanGeneralLedgerBridge {

    private static final Logger log = LoggerFactory.getLogger(LoanGeneralLedgerBridge.class);

    private final Optional<GeneralLedgerService> generalLedgerService;

    public LoanGeneralLedgerBridge(ObjectProvider<GeneralLedgerService> generalLedgerServiceProvider) {
        this.generalLedgerService = Optional.ofNullable(generalLedgerServiceProvider.getIfAvailable());
    }

    public void postInterestAccrual(UUID loanAccountId, LocalDate accrualDate, BigDecimal amount, String currency,
            String createdBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (generalLedgerService.isEmpty()) {
            return;
        }
        GeneralLedgerService gl = generalLedgerService.get();
        try {
            UUID receivable = gl.getOperationalGLAccount(OperationalGLAccountType.LOAN_INTEREST_RECEIVABLE.name());
            UUID income = gl.getOperationalGLAccount(OperationalGLAccountType.INTEREST_INCOME.name());
            String ref = "LOAN-ACCRUAL-" + loanAccountId + "-" + accrualDate;
            if (gl.getTransactionByReference(ref).isPresent()) {
                return;
            }
            PostTransactionCommand cmd = new PostTransactionCommand();
            cmd.setReferenceId(ref);
            cmd.setDescription("Loan interest accrual " + loanAccountId);
            cmd.setTransactionDate(accrualDate);
            cmd.setCurrency(currency);
            cmd.setCreatedBy(createdBy != null ? createdBy : "loan-service");
            PostTransactionCommand.JournalEntryCommand dr = new PostTransactionCommand.JournalEntryCommand();
            dr.setAccountId(receivable);
            dr.setDebitAmount(amount);
            dr.setCreditAmount(BigDecimal.ZERO);
            dr.setDescription("Accrued loan interest receivable");
            dr.setValueDate(accrualDate);
            PostTransactionCommand.JournalEntryCommand cr = new PostTransactionCommand.JournalEntryCommand();
            cr.setAccountId(income);
            cr.setDebitAmount(BigDecimal.ZERO);
            cr.setCreditAmount(amount);
            cr.setDescription("Loan interest income accrual");
            cr.setValueDate(accrualDate);
            cmd.setEntries(List.of(dr, cr));
            gl.postTransaction(cmd);
        } catch (Exception e) {
            log.warn(
                    "Skipping GL post for loan interest accrual {} {}: {}",
                    loanAccountId,
                    accrualDate,
                    e.getMessage());
        }
    }
}
