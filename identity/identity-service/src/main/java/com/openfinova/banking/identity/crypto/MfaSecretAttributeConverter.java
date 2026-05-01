package com.openfinova.banking.identity.crypto;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link com.openfinova.banking.identity.entity.BankingUser#mfaSecret} between plaintext (entity)
 * and ciphertext (database).
 */
@Component
@Converter(autoApply = false)
public class MfaSecretAttributeConverter implements AttributeConverter<String, String> {

    private final MfaSecretEncryptionService encryption;

    public MfaSecretAttributeConverter(MfaSecretEncryptionService encryption) {
        this.encryption = encryption;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryption.encryptToColumn(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryption.decryptFromColumn(dbData);
    }
}
