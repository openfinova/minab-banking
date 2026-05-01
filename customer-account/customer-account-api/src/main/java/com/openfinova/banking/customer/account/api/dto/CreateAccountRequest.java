package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to create a new account")
public class CreateAccountRequest {

    @NotNull(message = "User profile ID is required")
    @Schema(description = "Primary user profile ID", required = true)
    private UUID primaryUserProfileId;

    @NotNull(message = "Product type is required")
    @Schema(description = "Account product type", required = true)
    private AccountProductType productType;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Schema(description = "Three-letter ISO currency code", required = true, example = "USD")
    private String currency;

    @NotBlank(message = "Created by is required")
    @Schema(description = "User creating the account", required = true)
    private String createdBy;

    // Getters and setters
    public UUID getPrimaryUserProfileId() {
        return primaryUserProfileId;
    }

    public void setPrimaryUserProfileId(UUID primaryUserProfileId) {
        this.primaryUserProfileId = primaryUserProfileId;
    }

    public AccountProductType getProductType() {
        return productType;
    }

    public void setProductType(AccountProductType productType) {
        this.productType = productType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
