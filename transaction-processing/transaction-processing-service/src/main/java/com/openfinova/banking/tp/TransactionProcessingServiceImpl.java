package com.openfinova.banking.tp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.tp.api.TransactionProcessingService;
import com.openfinova.banking.tp.api.dto.BalanceReservationResponse;
import com.openfinova.banking.tp.api.dto.FeeCalculationResult;
import com.openfinova.banking.tp.api.dto.TransactionRequestDTO;
import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.api.dto.VelocityLimitStatus;
import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.ReservationType;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionRequest;
import com.openfinova.banking.tp.mapper.BalanceReservationMapper;
import com.openfinova.banking.tp.mapper.TransactionMapper;
import com.openfinova.banking.tp.service.BalanceReservationService;
import com.openfinova.banking.tp.service.FeeManagementService;
import com.openfinova.banking.tp.service.TransactionService;
import com.openfinova.banking.tp.service.VelocityLimitService;

/**
 * Facade implementation for Transaction Processing operations consumed by other modules.
 * Delegates to internal services and maps between internal entities and API DTOs.
 */
@Service
@Transactional
public class TransactionProcessingServiceImpl implements TransactionProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessingServiceImpl.class);

    private final TransactionService transactionService;
    private final BalanceReservationService balanceReservationService;
    private final VelocityLimitService velocityLimitService;
    private final FeeManagementService feeManagementService;
    private final TransactionMapper transactionMapper;
    private final BalanceReservationMapper balanceReservationMapper;

    public TransactionProcessingServiceImpl(TransactionService transactionService,
            BalanceReservationService balanceReservationService, VelocityLimitService velocityLimitService,
            FeeManagementService feeManagementService, TransactionMapper transactionMapper,
            BalanceReservationMapper balanceReservationMapper) {
        this.transactionService = transactionService;
        this.balanceReservationService = balanceReservationService;
        this.velocityLimitService = velocityLimitService;
        this.feeManagementService = feeManagementService;
        this.transactionMapper = transactionMapper;
        this.balanceReservationMapper = balanceReservationMapper;
    }

    @Override
    public TransactionResponse initiateTransaction(TransactionRequestDTO requestDTO) {
        logger.info("Facade: initiating transaction with idempotency key: {}", requestDTO.getIdempotencyKey());
        TransactionRequest request = mapToTransactionRequest(requestDTO);
        Transaction transaction = transactionService.initiateTransaction(request);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    public TransactionResponse processTransaction(UUID transactionId) {
        logger.info("Facade: processing transaction: {}", transactionId);
        Transaction transaction = transactionService.processTransaction(transactionId);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    public TransactionResponse completeTransaction(UUID transactionId) {
        logger.info("Facade: completing transaction: {}", transactionId);
        Transaction transaction = transactionService.completeTransaction(transactionId);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    public TransactionResponse failTransaction(UUID transactionId, String reason, String errorCode) {
        logger.warn("Facade: failing transaction: {} reason: {}", transactionId, reason);
        Transaction transaction = transactionService.failTransaction(transactionId, reason, errorCode);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        logger.debug("Facade: getting transaction: {}", id);
        Transaction transaction = transactionService.getTransactionById(id);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTransactionStatus(UUID transactionId) {
        logger.debug("Facade: getting transaction status: {}", transactionId);
        return transactionService.getTransactionStatus(transactionId).name();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionResponse> findExistingTransaction(String idempotencyKey) {
        logger.debug("Facade: finding existing transaction for key: {}", idempotencyKey);
        return transactionService.findExistingTransaction(idempotencyKey).map(transactionMapper::toResponse);
    }

    @Override
    public UUID reserveBalanceForTransaction(UUID transactionId, UUID accountId, BigDecimal amount,
            String reservationType) {
        logger.info(
                "Facade: reserving balance for transaction: {} account: {} amount: {}",
                transactionId,
                accountId,
                amount);
        Transaction transaction = transactionService.getTransactionById(transactionId);
        ReservationType type = ReservationType.valueOf(reservationType);
        return balanceReservationService.reserveBalanceForTransaction(transaction, accountId, amount, type);
    }

    @Override
    public void releaseReservation(UUID reservationId) {
        logger.info("Facade: releasing reservation: {}", reservationId);
        balanceReservationService.releaseReservation(reservationId);
    }

    @Override
    public void confirmReservation(UUID reservationId) {
        logger.info("Facade: confirming reservation: {}", reservationId);
        balanceReservationService.confirmReservation(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalReservedAmount(UUID accountId) {
        logger.debug("Facade: getting total reserved amount for account: {}", accountId);
        return balanceReservationService.getTotalReservedAmount(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceReservationResponse> getActiveReservations(UUID accountId) {
        logger.debug("Facade: getting active reservations for account: {}", accountId);
        return balanceReservationService.getActiveReservations(accountId).stream()
                .map(balanceReservationMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkLimits(UUID accountId, String transactionType, BigDecimal amount, String currency) {
        logger.debug("Facade: checking limits for account: {} type: {}", accountId, transactionType);
        TransactionType type = TransactionType.valueOf(transactionType);
        return velocityLimitService.checkLimits(accountId, type, amount, currency);
    }

    @Override
    public void incrementUsage(UUID accountId, String transactionType, BigDecimal amount, String currency) {
        logger.debug("Facade: incrementing usage for account: {} type: {}", accountId, transactionType);
        TransactionType type = TransactionType.valueOf(transactionType);
        velocityLimitService.incrementUsage(accountId, type, amount, currency);
    }

    @Override
    @Transactional(readOnly = true)
    public VelocityLimitStatus getCurrentLimitStatus(UUID accountId, String transactionType) {
        logger.debug("Facade: getting limit status for account: {} type: {}", accountId, transactionType);
        TransactionType type = TransactionType.valueOf(transactionType);
        return velocityLimitService.getCurrentLimitStatus(accountId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingLimit(UUID accountId, String transactionType, String limitPeriod) {
        logger.debug(
                "Facade: getting remaining limit for account: {} type: {} period: {}",
                accountId,
                transactionType,
                limitPeriod);
        TransactionType type = TransactionType.valueOf(transactionType);
        VelocityLimitPeriod period = VelocityLimitPeriod.valueOf(limitPeriod);
        return velocityLimitService.getRemainingLimit(accountId, type, period);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFees(UUID transactionId) {
        logger.debug("Facade: calculating fees for transaction: {}", transactionId);
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return feeManagementService.calculateFees(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeCalculationResult calculateDetailedFees(UUID transactionId) {
        logger.debug("Facade: calculating detailed fees for transaction: {}", transactionId);
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return feeManagementService.calculateDetailedFees(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerTier evaluateTierEligibility(UUID customerId) {
        logger.debug("Facade: evaluating tier eligibility for customer: {}", customerId);
        return feeManagementService.evaluateTierEligibility(customerId);
    }

    private TransactionRequest mapToTransactionRequest(TransactionRequestDTO dto) {
        TransactionRequest request = new TransactionRequest();
        request.setIdempotencyKey(dto.getIdempotencyKey());
        request.setTransactionType(TransactionType.valueOf(dto.getTransactionType().name()));
        request.setSourceAccountId(dto.getSourceAccountId());
        request.setDestinationAccountId(dto.getDestinationAccountId());
        request.setAmount(dto.getAmount());
        request.setCurrency(dto.getCurrency());
        request.setDescription(dto.getDescription());
        request.setCreatedBy(dto.getCreatedBy());
        return request;
    }
}
