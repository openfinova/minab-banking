package com.openfinova.banking.tan.crypto;

import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA bridge that transparently encrypts {@link com.openfinova.banking.tan.entity.TanDevice#tanSecret}
 * on write and decrypts on read.
 *
 * Keeps encryption concerns out of {@link com.openfinova.banking.tan.service.TanDeviceService} and
 * {@link com.openfinova.banking.tan.service.TanAuthorizationService}: application code works with
 * plaintext Base64 secrets in the entity while the database column always stores
 * {@link TanSecretEncryptionService} ciphertext. {@code autoApply = false} limits conversion to
 * explicitly annotated fields only.
 *
 * @see TanSecretEncryptionService
 */
@Component
@Converter(autoApply = false)
public class TanSecretAttributeConverter implements AttributeConverter<String, String> {

    private final TanSecretEncryptionService encryptionService;

    public TanSecretAttributeConverter(TanSecretEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encryptToColumn(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decryptFromColumn(dbData);
    }
}
