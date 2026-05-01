package com.openfinova.banking.setup.api.dto;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bank properties and configuration")
public class BankProperties {
    @Schema(description = "Bank name", example = "OpenFinova Bank")
    private String name;

    @ValidCurrency
    @Schema(description = "Primary operating currency", example = "USD")
    private String currency;

    @Schema(description = "SWIFT/BIC code", example = "OPFIUS33")
    private String swiftCode;

    @Schema(description = "ISO 3166-1 alpha-2 country code", example = "US")
    private String countryCode;

    public BankProperties() {
    }

    public BankProperties(String name, String currency, String swiftCode, String countryCode) {
        this.name = name;
        this.currency = currency;
        this.swiftCode = swiftCode;
        this.countryCode = countryCode;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
