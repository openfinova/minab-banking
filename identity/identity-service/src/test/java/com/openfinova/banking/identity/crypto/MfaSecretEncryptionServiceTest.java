package com.openfinova.banking.identity.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class MfaSecretEncryptionServiceTest {

    @Test
    void roundTrip_plaintextSecret() {
        MfaSecretEncryptionService svc = new MfaSecretEncryptionService("unit-test-key-material");
        String plain = "JBSWY3DPEHPK3PXP";
        String column = svc.encryptToColumn(plain);
        assertThat(column).startsWith(MfaSecretEncryptionService.PREFIX);
        assertThat(svc.decryptFromColumn(column)).isEqualTo(plain);
    }

    @Test
    void legacyPlaintext_withoutPrefixReturnedAsIs() {
        MfaSecretEncryptionService svc = new MfaSecretEncryptionService("unit-test-key-material");
        String legacy = "JBSWY3DPEHPK3PXP";
        assertThat(svc.decryptFromColumn(legacy)).isSameAs(legacy);
    }

    @Test
    void blankKeyRejected() {
        assertThatThrownBy(() -> new MfaSecretEncryptionService("  ")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity.mfa-secret.encryption.key");
    }

    @Test
    void nullStaysNull() {
        MfaSecretEncryptionService svc = new MfaSecretEncryptionService("k");
        assertThat(svc.encryptToColumn(null)).isNull();
        assertThat(svc.decryptFromColumn(null)).isNull();
    }
}
