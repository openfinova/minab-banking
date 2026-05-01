package com.openfinova.banking.customer.account.entity;

import com.openfinova.banking.common.lib.converter.SetToJsonConverter;
import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.Set;

@Converter
public class AccountPermissionSetConverter extends SetToJsonConverter<AccountPermission> {

    @Override
    protected TypeReference<Set<AccountPermission>> getTypeReference() {
        return new TypeReference<Set<AccountPermission>>() {
        };
    }
}
