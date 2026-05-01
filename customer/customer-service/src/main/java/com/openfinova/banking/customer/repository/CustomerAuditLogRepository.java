package com.openfinova.banking.customer.repository;

import com.openfinova.banking.customer.entity.CustomerAuditAction;
import com.openfinova.banking.customer.entity.CustomerAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CustomerAuditLogRepository extends JpaRepository<CustomerAuditLog, UUID> {

    /**
     * Paginated audit trail for a customer, newest first.
     */
    Page<CustomerAuditLog> findByCustomerIdOrderByChangedAtDesc(UUID customerId, Pageable pageable);

    /**
     * Filter audit log by customer and action type.
     */
    List<CustomerAuditLog> findByCustomerIdAndAction(UUID customerId, CustomerAuditAction action);

    /**
     * Filter audit log by customer, action and field name.
     */
    List<CustomerAuditLog> findByCustomerIdAndActionAndFieldName(UUID customerId, CustomerAuditAction action,
            String fieldName);

    /**
     * Audit trail for a customer within a specific time window.
     */
    @Query("SELECT a FROM CustomerAuditLog a WHERE a.customer.id = :customerId "
            + "AND a.changedAt >= :from AND a.changedAt <= :to ORDER BY a.changedAt DESC")
    List<CustomerAuditLog> findByCustomerIdAndTimestampRange(@Param("customerId") UUID customerId,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Find entries by correlation ID for distributed tracing.
     */
    List<CustomerAuditLog> findByCorrelationId(String correlationId);
}
