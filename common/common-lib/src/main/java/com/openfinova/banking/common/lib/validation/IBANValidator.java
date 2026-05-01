package com.openfinova.banking.common.lib.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigInteger;

public class IBANValidator implements ConstraintValidator<IBAN, String> {

    @Override
    public boolean isValid(String iban, ConstraintValidatorContext context) {
        if (iban == null || iban.trim().isEmpty()) {
            return true; // Use @NotNull or @NotBlank for null checks
        }

        // 1. Remove spaces and convert to uppercase
        String trimmed = iban.replaceAll("\\s+", "").toUpperCase();

        // 2. Check length (min 15, max 34 per standard)
        if (trimmed.length() < 15 || trimmed.length() > 34) {
            return false;
        }

        // 3. Check for illegal characters (only A-Z and 0-9 allowed)
        if (!trimmed.matches("[A-Z0-9]+")) {
            return false;
        }

        // 4. Move first 4 characters to the end
        String reordered = trimmed.substring(4) + trimmed.substring(0, 4);

        // 5. Convert letters to numbers (A=10, B=11, ..., Z=35)
        StringBuilder numeric = new StringBuilder();
        for (char c : reordered.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                numeric.append(c - 'A' + 10);
            }
        }

        // 6. Calculate Mod 97 (should be 1)
        BigInteger bigInt = new BigInteger(numeric.toString());
        return bigInt.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
