package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.dto.GLJournalEntryResponse;
import com.openfinova.banking.gl.api.dto.GLTransactionResponse;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.entity.GLTransaction;
import org.springframework.stereotype.Component;

@Component
public class GLTransactionMapper {

    public GLTransactionResponse toResponse(GLTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        GLTransactionResponse response = new GLTransactionResponse();
        response.setId(transaction.getId());
        response.setReferenceId(transaction.getReferenceId());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setDescription(transaction.getDescription());
        response.setStatus(transaction.getStatus());
        response.setCurrency(transaction.getCurrency());
        response.setCreatedBy(transaction.getCreatedBy());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setPostingDate(transaction.getPostingDate());

        if (transaction.getJournalEntries() != null) {
            response.setJournalEntries(
                    transaction.getJournalEntries().stream().map(this::toJournalEntryResponse).toList());
        }

        return response;
    }

    public GLJournalEntryResponse toJournalEntryResponse(GLJournalEntry entry) {
        if (entry == null) {
            return null;
        }

        GLJournalEntryResponse response = new GLJournalEntryResponse();
        response.setId(entry.getId());
        response.setAccountId(entry.getAccount().getId());
        response.setAccountCode(entry.getAccount().getCode());
        response.setAccountName(entry.getAccount().getName());
        response.setEntryType(entry.getEntryType());
        response.setDebitAmount(entry.getDebitAmount());
        response.setCreditAmount(entry.getCreditAmount());
        response.setCurrency(entry.getCurrency());
        response.setDescription(entry.getDescription());
        response.setLineNumber(entry.getLineNumber());

        return response;
    }
}
