package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;

/**
 * Account export data.
 */
public class AccountExportData {
    private String code;
    private String name;
    private GLAccountType type;
    private String currency;
    private String parentCode;
    private GLAccountStatus status;
    private BalanceType normalBalance;
    private String description;
    private OperationalGLAccountType operationalAccountType;

    public AccountExportData() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GLAccountType getType() {
        return type;
    }

    public void setType(GLAccountType type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public GLAccountStatus getStatus() {
        return status;
    }

    public void setStatus(GLAccountStatus status) {
        this.status = status;
    }

    public BalanceType getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(BalanceType normalBalance) {
        this.normalBalance = normalBalance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OperationalGLAccountType getOperationalAccountType() {
        return operationalAccountType;
    }

    public void setOperationalAccountType(OperationalGLAccountType operationalAccountType) {
        this.operationalAccountType = operationalAccountType;
    }
}
