package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transaction validation response")
public class TransactionValidationResponse {

    @Schema(description = "Transaction ID")
    private UUID transactionId;

    @Schema(description = "Whether the transaction is balanced")
    private boolean balanced;

    // Constructors
    public TransactionValidationResponse() {
    }

    // Getters and Setters
    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }
}
