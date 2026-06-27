package com.openfinova.banking.customer.account.api.dto;

/**
 * Minimal payee details for payment authorization displays (IBAN + display name).
 */
public record AccountPayeeInfo(String iban, String displayName) {
}
