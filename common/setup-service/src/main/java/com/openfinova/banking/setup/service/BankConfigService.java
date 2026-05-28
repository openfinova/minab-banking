package com.openfinova.banking.setup.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.dto.BankProperties;
import com.openfinova.banking.setup.config.BankConfigProperties;

@Service
public class BankConfigService {

    private final BankConfigProperties config;

    public BankConfigService(BankConfigProperties config) {
        this.config = config;
    }

    @PreAuthorize("hasAnyAuthority('admin:config:read', 'service:setup:read')")
    public BankProperties getBankDetails() {
        return new BankProperties(
                config.getName(),
                config.getCurrency(),
                config.getSwiftCode(),
                config.getCountryCode());
    }

    @PreAuthorize("hasAnyAuthority('admin:config:read', 'service:setup:read')")
    public String getBankName() {
        return config.getName();
    }

    @PreAuthorize("hasAnyAuthority('admin:config:read', 'service:setup:read')")
    public String getCurrency() {
        return config.getCurrency();
    }
}
