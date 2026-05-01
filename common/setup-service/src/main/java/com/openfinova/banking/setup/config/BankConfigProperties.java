package com.openfinova.banking.setup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.openfinova.banking.common.lib.validation.ValidCurrency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ConfigurationProperties(prefix = "banking.setup")
@Validated
public class BankConfigProperties {

    @NotBlank(message = "Bank name must not be blank")
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String name;

    @ValidCurrency
    private String currency;

    /**
     * ISO 9362 BIC: 4 letter bank code + 2 letter country + 2 alphanumeric
     * location + optional 3 alphanumeric branch
     */
    @NotBlank(message = "SWIFT/BIC code must not be blank")
    @Pattern(regexp = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "SWIFT/BIC code must be 8 or 11 uppercase alphanumeric characters (ISO 9362)")
    private String swiftCode;

    /**
     * ISO 3166-1 alpha-2 two-letter country code
     */
    @NotBlank(message = "Country code must not be blank")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be exactly 2 uppercase letters (ISO 3166-1 alpha-2)")
    private String countryCode;

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
