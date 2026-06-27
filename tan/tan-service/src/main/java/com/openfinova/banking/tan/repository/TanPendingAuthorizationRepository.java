package com.openfinova.banking.tan.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.tan.entity.TanPendingAuthorization;

public interface TanPendingAuthorizationRepository extends JpaRepository<TanPendingAuthorization, UUID> {

    Optional<TanPendingAuthorization> findByTransactionId(UUID transactionId);

    Optional<TanPendingAuthorization> findByTransactionIdAndUserId(UUID transactionId, UUID userId);
}
