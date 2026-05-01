package com.openfinova.banking.gl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.gl.api.entity.GLTransactionStatus;
import com.openfinova.banking.gl.entity.GLTransaction;

public interface GLTransactionRepository extends JpaRepository<GLTransaction, UUID> {

    /**
     * Find transaction by reference ID.
     *
     * @param referenceId the reference ID
     * @return optional containing the transaction if found
     */
    Optional<GLTransaction> findByReferenceId(String referenceId);

    /**
     * Find transactions by date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of transactions in the date range
     */
    @Query("SELECT t FROM GeneralLedgerTransaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate, t.transactionNumber")
    List<GLTransaction> findByTransactionDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions with transaction numbers ordered by transaction number.
     *
     * @return list of transactions with transaction numbers
     */
    @Query("SELECT t FROM GeneralLedgerTransaction t WHERE t.transactionNumber IS NOT NULL ORDER BY t.transactionNumber")
    List<GLTransaction> findAllWithTransactionNumbersOrdered();

    /**
     * Find the maximum transaction number.
     *
     * @return optional containing the maximum transaction number
     */
    @Query("SELECT MAX(t.transactionNumber) FROM GeneralLedgerTransaction t WHERE t.transactionNumber IS NOT NULL")
    Optional<Long> findMaxTransactionNumber();

    /**
     * Find transactions by status.
     *
     * @param status the transaction status
     * @return list of transactions with the specified status
     */
    @Query("SELECT t FROM GeneralLedgerTransaction t WHERE t.status = :status ORDER BY t.transactionDate DESC")
    List<GLTransaction> findByStatus(@Param("status") String status);

    /**
     * Count transactions by date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return count of transactions in the date range
     */
    @Query("SELECT COUNT(t) FROM GeneralLedgerTransaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    long countByTransactionDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find transactions by status and date range.
     * Used for period-close validation (finding pending approvals in a fiscal period).
     *
     * @param status the transaction status
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of transactions matching criteria
     */
    @Query("""
            SELECT t FROM GeneralLedgerTransaction t
            WHERE t.status = :status
            AND t.transactionDate BETWEEN :startDate AND :endDate
            ORDER BY t.transactionDate, t.createdAt
            """)
    List<GLTransaction> findByStatusAndTransactionDateBetween(@Param("status") GLTransactionStatus status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
