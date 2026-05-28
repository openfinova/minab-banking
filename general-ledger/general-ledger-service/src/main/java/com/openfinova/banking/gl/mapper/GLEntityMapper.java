package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.dto.*;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.service.GLAccountService;

import java.util.List;

/**
 * Mapper utility for converting GL entities to DTOs.
 * Used by the GeneralLedgerFacade to prevent entity leakage to external modules.
 */
public class GLEntityMapper {

    private GLEntityMapper() {
        // Utility class
    }

    /**
     * Converts GLAccount entity to DTO.
     */
    public static GLAccountDTO toDTO(GLAccount entity) {
        if (entity == null) {
            return null;
        }

        return new GLAccountDTO(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getType(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getNormalBalance(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getDescription());
    }

    /**
     * Converts list of GLAccount entities to DTOs.
     */
    public static List<GLAccountDTO> toAccountDTOList(List<GLAccount> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(GLEntityMapper::toDTO).toList();
    }

    /**
     * Converts GLTransaction entity to DTO.
     */
    public static GLTransactionDTO toDTO(GLTransaction entity) {
        if (entity == null) {
            return null;
        }

        List<GLJournalEntryDTO> entryDTOs = entity.getJournalEntries().stream().map(GLEntityMapper::toDTO).toList();

        return new GLTransactionDTO(
                entity.getId(),
                entity.getReferenceId(),
                entity.getTransactionNumber(),
                entity.getDescription(),
                entity.getTransactionDate(),
                entity.getCurrency(),
                entity.getStatus(),
                entryDTOs);
    }

    /**
     * Converts GLJournalEntry entity to DTO.
     */
    public static GLJournalEntryDTO toDTO(GLJournalEntry entity) {
        if (entity == null) {
            return null;
        }

        return new GLJournalEntryDTO(
                entity.getId(),
                entity.getTransaction() != null ? entity.getTransaction().getId() : null,
                entity.getAccount() != null ? entity.getAccount().getId() : null,
                entity.getAccount() != null ? entity.getAccount().getCode() : null,
                entity.getDebitAmount(),
                entity.getCreditAmount(),
                entity.getDescription(),
                entity.getLineNumber(),
                entity.getValueDate(),
                entity.getCurrency());
    }

    /**
     * Converts list of GLJournalEntry entities to DTOs.
     */
    public static List<GLJournalEntryDTO> toEntryDTOList(List<GLJournalEntry> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(GLEntityMapper::toDTO).toList();
    }

    /**
     * Converts FiscalPeriod entity to DTO.
     */
    public static FiscalPeriodDTO toDTO(FiscalPeriod entity) {
        if (entity == null) {
            return null;
        }

        return new FiscalPeriodDTO(
                entity.getId(),
                entity.getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus());
    }

    /**
     * Converts list of FiscalPeriod entities to DTOs.
     */
    public static List<FiscalPeriodDTO> toPeriodDTOList(List<FiscalPeriod> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(GLEntityMapper::toDTO).toList();
    }

    /**
     * Converts PostTransactionCommand to GLTransaction entity.
     * Requires GLAccountService to fetch account entities.
     */
    public static GLTransaction toEntity(PostTransactionCommand command, GLAccountService accountService) {
        if (command == null) {
            return null;
        }

        GLTransaction transaction = new GLTransaction(
                command.getReferenceId(),
                command.getDescription(),
                command.getTransactionDate());
        transaction.setCurrency(command.getCurrency());

        // Add journal entries
        if (command.getEntries() != null) {
            int lineNum = 1;
            for (PostTransactionCommand.JournalEntryCommand entryCmd : command.getEntries()) {
                GLAccount account = accountService.getAccountById(entryCmd.getAccountId()).orElseThrow(
                        () -> new IllegalArgumentException("Account not found: " + entryCmd.getAccountId()));

                GLJournalEntry entry = new GLJournalEntry(
                        account,
                        entryCmd.getDebitAmount(),
                        entryCmd.getCreditAmount(),
                        entryCmd.getDescription(),
                        entryCmd.getValueDate());

                String entryCurrency = entryCmd.getCurrency() != null ? entryCmd.getCurrency() : command.getCurrency();
                entry.setCurrency(entryCurrency);
                entry.setLineNumber(lineNum++);

                java.math.BigDecimal rate = entryCmd.getExchangeRate() != null ? entryCmd.getExchangeRate()
                        : java.math.BigDecimal.ONE;
                entry.setExchangeRate(rate);
                // In a valid double-entry line exactly one of debitAmount/creditAmount is null.
                // Guard against NPE before multiplying by the FX rate.
                entry.setBaseDebitAmount(
                        entryCmd.getDebitAmount() != null ? entryCmd.getDebitAmount().multiply(rate)
                                : java.math.BigDecimal.ZERO);
                entry.setBaseCreditAmount(
                        entryCmd.getCreditAmount() != null ? entryCmd.getCreditAmount().multiply(rate)
                                : java.math.BigDecimal.ZERO);

                transaction.addGLJournalEntry(entry);
            }
        }

        return transaction;
    }

}
