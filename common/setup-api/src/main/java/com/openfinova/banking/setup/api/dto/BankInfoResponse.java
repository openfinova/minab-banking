package com.openfinova.banking.setup.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for bank information.
 */
@Schema(description = "Response containing bank information")
public class BankInfoResponse {
    @Schema(description = "Bank name", example = "OpenFinova Bank")
    private String bankName;

    public BankInfoResponse() {
    }

    public BankInfoResponse(String bankName) {
        this.bankName = bankName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
}
