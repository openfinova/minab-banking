package com.openfinova.banking.customer.repository;

import com.openfinova.banking.customer.api.entity.DataSubjectRequestStatus;
import com.openfinova.banking.customer.api.entity.DataSubjectRequestType;
import com.openfinova.banking.customer.entity.DataSubjectRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, UUID> {

    /**
     * All requests for a given customer, newest first.
     */
    List<DataSubjectRequest> findByCustomerIdOrderByReceivedAtDesc(UUID customerId);

    /**
     * All requests in a given status.
     */
    List<DataSubjectRequest> findByStatus(DataSubjectRequestStatus status);

    /**
     * All requests of a specific type for a customer.
     */
    List<DataSubjectRequest> findByCustomerIdAndRequestType(UUID customerId, DataSubjectRequestType requestType);

    /**
     * Lookup by reference number (given to the customer at submission time).
     */
    Optional<DataSubjectRequest> findByReferenceNumber(String referenceNumber);

    /**
     * Find all open requests that have passed their due date — for SLA breach alerts.
     */
    @Query("""
            SELECT r FROM DataSubjectRequest r WHERE r.status NOT IN ('FULFILLED', 'REJECTED', 'WITHDRAWN')
            AND r.dueBy < :today ORDER BY r.dueBy ASC
            """)
    List<DataSubjectRequest> findOverdueRequests(@Param("today") LocalDate today);

    /**
     * Find all DEFERRED requests whose deferral end date has been reached
     * (i.e. the retention period has now expired). The scheduler uses this
     * to auto-fulfill deferred erasure requests.
     */
    @Query("SELECT r FROM DataSubjectRequest r " + "WHERE r.status = 'DEFERRED' AND r.deferredUntil <= :today")
    List<DataSubjectRequest> findReadyToProcessDeferredRequests(@Param("today") LocalDate today);

    /**
     * Count open requests for a customer (for duplicate/spam detection).
     */
    @Query("""
            SELECT COUNT(r) FROM DataSubjectRequest r WHERE r.customer.id = :customerId
            AND r.status NOT IN ('FULFILLED', 'REJECTED', 'WITHDRAWN')
            """)
    long countOpenRequestsForCustomer(@Param("customerId") UUID customerId);
}
