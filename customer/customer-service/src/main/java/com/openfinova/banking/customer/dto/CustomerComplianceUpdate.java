package com.openfinova.banking.customer.dto;

/**
 * Partial update for risk flags. Null means "leave unchanged".
 */
public class CustomerComplianceUpdate {

    private Boolean pepFlag;
    private Boolean sanctionFlag;

    public CustomerComplianceUpdate() {
    }

    public Boolean getPepFlag() {
        return pepFlag;
    }

    public void setPepFlag(Boolean pepFlag) {
        this.pepFlag = pepFlag;
    }

    public Boolean getSanctionFlag() {
        return sanctionFlag;
    }

    public void setSanctionFlag(Boolean sanctionFlag) {
        this.sanctionFlag = sanctionFlag;
    }
}
