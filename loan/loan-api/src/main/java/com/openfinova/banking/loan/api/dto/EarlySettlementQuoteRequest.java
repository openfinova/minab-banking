package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.SettlementCalculationMethod;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for generating an early settlement quote.
 */
public class EarlySettlementQuoteRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Settlement date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate settlementDate;

    @NotNull(message = "Calculation method is required")
    private SettlementCalculationMethod calculationMethod;

    private String requestedBy;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public SettlementCalculationMethod getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(SettlementCalculationMethod calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
}
