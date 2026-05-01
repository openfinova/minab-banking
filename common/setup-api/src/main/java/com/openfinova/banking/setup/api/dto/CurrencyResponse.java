package com.openfinova.banking.setup.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for currency information.
 */
@Schema(description = "Response containing currency code")
public class CurrencyResponse {
    @Schema(description = "Three-letter ISO currency code", example = "USD")
    private String currency;

    public CurrencyResponse() {
    }

    public CurrencyResponse(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
