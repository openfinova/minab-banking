package com.openfinova.banking.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.LoanService;
import com.openfinova.banking.loan.api.dto.LoanAccountResponse;
import com.openfinova.banking.loan.api.dto.LoanDisbursementResponse;
import com.openfinova.banking.loan.api.dto.LoanPaymentResponse;
import com.openfinova.banking.loan.api.entity.PaymentMethod;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanDisbursement;
import com.openfinova.banking.loan.entity.LoanPayment;
import com.openfinova.banking.loan.mapper.LoanAccountMapper;
import com.openfinova.banking.loan.mapper.LoanDisbursementMapper;
import com.openfinova.banking.loan.mapper.LoanPaymentMapper;
import com.openfinova.banking.loan.service.LoanAccountService;
import com.openfinova.banking.loan.service.LoanDisbursementService;
import com.openfinova.banking.loan.service.LoanPaymentService;

/**
 * Facade implementation for {@link LoanService}. Delegates to internal domain services
 * while exposing only API DTOs to other modules.
 */
@Service
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private final LoanAccountService loanAccountService;
    private final LoanDisbursementService loanDisbursementService;
    private final LoanPaymentService loanPaymentService;

    public LoanServiceImpl(LoanAccountService loanAccountService, LoanDisbursementService loanDisbursementService,
            LoanPaymentService loanPaymentService) {
        this.loanAccountService = loanAccountService;
        this.loanDisbursementService = loanDisbursementService;
        this.loanPaymentService = loanPaymentService;
    }

    @Override
    public Optional<LoanAccountResponse> getLoanAccountById(UUID loanAccountId) {
        return loanAccountService.getLoanAccountById(loanAccountId).map(LoanAccountMapper::toResponse);
    }

    @Override
    public Optional<LoanAccountResponse> getLoanAccountByNumber(String loanAccountNumber) {
        return loanAccountService.getLoanAccountByNumber(loanAccountNumber).map(LoanAccountMapper::toResponse);
    }

    @Override
    public boolean loanAccountExists(UUID loanAccountId) {
        return loanAccountService.getLoanAccountById(loanAccountId).isPresent();
    }

    @Override
    public Optional<UUID> getCustomerIdForLoanAccount(UUID loanAccountId) {
        return loanAccountService.getLoanAccountById(loanAccountId).map(LoanAccount::getCustomerId);
    }

    @Override
    public boolean isLoanAccountEligibleForRepayment(UUID loanAccountId) {
        return loanAccountService.isLoanAccountEligibleForRepayment(loanAccountId);
    }

    @Override
    public Optional<LoanDisbursementResponse> findDisbursementByReference(String disbursementReference) {
        return loanDisbursementService.getDisbursementByReference(disbursementReference)
                .map(LoanDisbursementMapper::toResponse);
    }

    @Override
    @Transactional
    public LoanDisbursementResponse completeDisbursementAfterTransfer(UUID disbursementId, String transactionReference,
            String completedBy) {
        LoanDisbursement saved = loanDisbursementService
                .completeDisbursementAfterTransfer(disbursementId, transactionReference, completedBy);
        return LoanDisbursementMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LoanDisbursementResponse failDisbursementAfterTransfer(UUID disbursementId, String failureReason,
            String failedBy) {
        LoanDisbursement failed = loanDisbursementService.failDisbursement(disbursementId, failureReason, failedBy);
        return LoanDisbursementMapper.toResponse(failed);
    }

    @Override
    public boolean repaymentExistsForTransactionReference(String transactionReference) {
        return loanPaymentService.repaymentExistsForTransactionReference(transactionReference);
    }

    @Override
    @Transactional
    public LoanPaymentResponse recordRepaymentFromPaymentSystem(UUID loanAccountId, BigDecimal amount,
            LocalDate valueDate, PaymentMethod paymentMethod, String transactionReference, String recordedBy) {
        LoanPayment payment = loanPaymentService
                .recordPayment(loanAccountId, amount, valueDate, paymentMethod, transactionReference, recordedBy);
        return LoanPaymentMapper.toResponse(payment);
    }
}
