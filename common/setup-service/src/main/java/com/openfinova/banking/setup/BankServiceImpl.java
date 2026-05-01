package com.openfinova.banking.setup;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.BankService;
import com.openfinova.banking.setup.api.dto.BankProperties;
import com.openfinova.banking.setup.config.BankConfigProperties;

@Service
public class BankServiceImpl implements BankService {

    private final BankConfigProperties config;

    public BankServiceImpl(BankConfigProperties config) {
        this.config = config;
    }

    @Override
    @PreAuthorize("hasAuthority('service:setup:read')")
    public BankProperties getBankDetails() {
        return new BankProperties(
                config.getName(),
                config.getCurrency(),
                config.getSwiftCode(),
                config.getCountryCode());
    }

    @Override
    @PreAuthorize("hasAuthority('service:setup:read')")
    public String getBankName() {
        return config.getName();
    }

    @Override
    @PreAuthorize("hasAuthority('service:setup:read')")
    public String getCurrency() {
        return config.getCurrency();
    }
}
