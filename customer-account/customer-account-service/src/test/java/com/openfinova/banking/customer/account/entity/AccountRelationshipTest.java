package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import com.openfinova.banking.customer.account.api.entity.RelationshipStatus;
import com.openfinova.banking.customer.account.api.entity.RelationshipType;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class AccountRelationshipTest {
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void hasPermission_andPrimaryHolder() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountRelationship primary = new AccountRelationship(
                a,
                java.util.UUID.randomUUID(),
                RelationshipType.PRIMARY_HOLDER,
                BASE_TIME);
        assertThat(primary.isPrimaryHolder()).isTrue();
        assertThat(primary.hasPermission(AccountPermission.VIEW)).isTrue();

        AccountRelationship auth = new AccountRelationship(
                a,
                java.util.UUID.randomUUID(),
                RelationshipType.AUTHORIZED_USER,
                BASE_TIME);
        assertThat(auth.isPrimaryHolder()).isFalse();
        assertThat(auth.hasPermission(AccountPermission.TRANSACT)).isTrue();
        assertThat(auth.hasPermission(AccountPermission.ADMIN)).isFalse();
    }

    @Test
    void isEffective_requiresActiveStatusAndDateWindow() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountRelationship rel = new AccountRelationship();
        rel.setCustomerAccount(a);
        rel.setUserProfileId(java.util.UUID.randomUUID());
        rel.setRelationshipType(RelationshipType.SECONDARY_HOLDER);
        rel.setCreatedBy("staff");
        rel.setEffectiveFrom(BASE_TIME.minusDays(1));
        rel.setStatus(RelationshipStatus.ACTIVE);
        assertThat(rel.isEffective(BASE_TIME)).isTrue();

        rel.setEffectiveUntil(BASE_TIME.minusHours(1));
        assertThat(rel.isEffective(BASE_TIME)).isFalse();
    }

    @Test
    void beneficiaryValidation_andMutators() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountRelationship rel = new AccountRelationship(
                a,
                java.util.UUID.randomUUID(),
                RelationshipType.AUTHORIZED_USER,
                BASE_TIME);
        rel.setIsBeneficiary(true);
        rel.setBeneficiaryPercentage(null);
        assertThatThrownBy(rel::validateBeneficiaryPercentage).isInstanceOf(IllegalArgumentException.class);

        rel.setBeneficiary(new BigDecimal("25"));
        assertThat(rel.getIsBeneficiary()).isTrue();
        assertThat(rel.getBeneficiaryPercentage()).isEqualByComparingTo("25");

        rel.removeBeneficiary();
        assertThat(rel.getIsBeneficiary()).isFalse();
        assertThat(rel.getBeneficiaryPercentage()).isNull();

        rel.setIsBeneficiary(false);
        rel.setBeneficiaryPercentage(new BigDecimal("10"));
        assertThatThrownBy(rel::validateBeneficiaryPercentage).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addAndRemovePermission() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountRelationship rel = new AccountRelationship(
                a,
                java.util.UUID.randomUUID(),
                RelationshipType.AUTHORIZED_USER,
                BASE_TIME);
        rel.setPermissions(new HashSet<>(rel.getPermissions()));
        rel.addPermission(AccountPermission.ADMIN);
        assertThat(rel.hasPermission(AccountPermission.ADMIN)).isTrue();
        rel.removePermission(AccountPermission.ADMIN);
        assertThat(rel.hasPermission(AccountPermission.ADMIN)).isFalse();
    }
}
