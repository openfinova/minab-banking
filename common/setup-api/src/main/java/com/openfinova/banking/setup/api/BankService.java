package com.openfinova.banking.setup.api;

import com.openfinova.banking.setup.api.dto.BankProperties;

public interface BankService {
    BankProperties getBankDetails();

    String getBankName();

    String getCurrency();
}
