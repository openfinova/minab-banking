package com.openfinova.banking.tp.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.tp.api.entity.ReservationStatus;
import com.openfinova.banking.tp.entity.BalanceReservation;

/**
 * Repository for BalanceReservation entities.
 */
public interface BalanceReservationRepository extends JpaRepository<BalanceReservation, UUID> {

    /**
     * Find a balance reservation by transaction ID (for idempotency: one reservation per transaction).
     */
    Optional<BalanceReservation> findByTransaction_Id(UUID transactionId);

    /**
     * Find a balance reservation by its unique idempotency key.
     *
     * @param reservationKey the unique reservation key
     * @return an optional containing the reservation if found
     */
    Optional<BalanceReservation> findByReservationKey(String reservationKey);

    /**
     * Find all balance reservations for an account with a specific status.
     *
     * @param accountId the account ID
     * @param status    the reservation status
     * @return a list of reservations
     */
    List<BalanceReservation> findByAccountIdAndStatus(UUID accountId, ReservationStatus status);

    /**
     * Find all active reservations for an account.
     *
     * @param accountId the account ID
     * @return a list of active reservations
     */
    @Query("SELECT br FROM BalanceReservation br WHERE br.accountId = :accountId AND br.status = 'ACTIVE'")
    List<BalanceReservation> findActiveReservationsByAccountId(@Param("accountId") UUID accountId);

    /**
     * Calculate total reserved amount for an account.
     *
     * @param accountId the account ID
     * @return the total reserved amount
     */
    @Query("""
            SELECT COALESCE(SUM(br.reservedAmount), 0) FROM BalanceReservation br
            WHERE br.accountId = :accountId
            AND br.status = 'ACTIVE'
            AND br.expiresAt > :currentTime
            """)
    BigDecimal getTotalReservedAmount(@Param("accountId") UUID accountId,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find expired reservations for an account.
     *
     * @param accountId the account ID
     * @param currentTime the current time
     * @return a list of expired reservations
     */
    @Query("""
            SELECT br FROM BalanceReservation br
            WHERE br.accountId = :accountId
            AND br.status = 'ACTIVE'
            AND br.expiresAt <= :currentTime
            """)
    List<BalanceReservation> findExpiredReservations(@Param("accountId") UUID accountId,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find reservations by multiple IDs for batch operations.
     *
     * @param reservationIds the list of reservation IDs
     * @return a list of reservations
     */
    List<BalanceReservation> findByIdIn(List<UUID> reservationIds);

    /**
     * Find all reservations that are active and not expired.
     *
     * @param currentTime the current time
     * @return a list of active, non-expired reservations
     */
    @Query("SELECT br FROM BalanceReservation br WHERE br.status = 'ACTIVE' AND br.expiresAt > :currentTime")
    List<BalanceReservation> findActiveNonExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find all expired reservations (ACTIVE with expiresAt &lt;= currentTime) across all accounts.
     * Used by the scheduler to release expired reservations and free locked funds.
     */
    @Query("""
            SELECT br FROM BalanceReservation br
            WHERE br.status = 'ACTIVE'
            AND br.expiresAt <= :currentTime
            """)
    List<BalanceReservation> findAllExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
}
