package com.openfinova.banking.identity.entity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.identity.api.model.UserType;

class BankingUserTest {

    @Test
    void isEffectivelySuspended_falseWhenNotSuspended() {
        BankingUser u = new BankingUser("alice", "hash", UserType.STAFF);
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        assertThat(u.isEffectivelySuspended(now)).isFalse();
    }

    @Test
    void isEffectivelySuspended_trueWhenSuspendedWithoutEnd() {
        BankingUser u = new BankingUser("alice", "hash", UserType.STAFF);
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);
        u.setSuspendedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        assertThat(u.isEffectivelySuspended(now)).isTrue();
    }

    @Test
    void isEffectivelySuspended_falseWhenTimedSuspensionHasEnded() {
        BankingUser u = new BankingUser("alice", "hash", UserType.STAFF);
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);
        u.setSuspendedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        u.setSuspensionUntil(LocalDateTime.of(2026, 6, 15, 10, 0));
        assertThat(u.isEffectivelySuspended(now)).isFalse();
    }

    @Test
    void isEffectivelySuspended_trueWhenSuspensionEndStillInFuture() {
        BankingUser u = new BankingUser("alice", "hash", UserType.STAFF);
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);
        u.setSuspendedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        u.setSuspensionUntil(LocalDateTime.of(2026, 6, 20, 0, 0));
        assertThat(u.isEffectivelySuspended(now)).isTrue();
    }
}
