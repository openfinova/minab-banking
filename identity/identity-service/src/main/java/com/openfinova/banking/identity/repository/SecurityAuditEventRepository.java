package com.openfinova.banking.identity.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.openfinova.banking.identity.entity.SecurityAuditEvent;

/**
 * Append-only persistence for audit rows: {@code save} and read/search only (no delete/update API).
 */
public interface SecurityAuditEventRepository
        extends JpaRepository<SecurityAuditEvent, UUID>, JpaSpecificationExecutor<SecurityAuditEvent> {
}
