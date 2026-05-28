package com.openfinova.banking.setup;

import org.springframework.stereotype.Service;

import com.openfinova.banking.setup.api.BankService;
import com.openfinova.banking.setup.api.dto.BankProperties;
import com.openfinova.banking.setup.service.BankConfigService;

@Service
public class BankServiceImpl implements BankService {

    private final BankConfigService bankConfigService;

    public BankServiceImpl(BankConfigService bankConfigService) {
        this.bankConfigService = bankConfigService;
    }

    @Override
    public BankProperties getBankDetails() {
        return bankConfigService.getBankDetails();
    }

    @Override
    public String getBankName() {
        return bankConfigService.getBankName();
    }

    @Override
    public String getCurrency() {
        return bankConfigService.getCurrency();
    }
}
