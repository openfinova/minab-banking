package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.ScheduleStatus;
import com.openfinova.banking.loan.entity.LoanSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanSchedule entities.
 */
public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, UUID> {

    /**
     * Find all schedules for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of schedules ordered by installment number
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            ORDER BY ls.installmentNumber ASC
            """)
    List<LoanSchedule> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find all schedules for a loan account with pagination.
     *
     * @param loanAccountId the loan account ID
     * @param pageable pagination information
     * @return page of schedules
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            ORDER BY ls.installmentNumber ASC
            """)
    Page<LoanSchedule> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId, Pageable pageable);

    /**
     * Find schedule by loan account and installment number.
     *
     * @param loanAccountId the loan account ID
     * @param installmentNumber the installment number
     * @return optional containing the schedule if found
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.installmentNumber = :installmentNumber
            """)
    Optional<LoanSchedule> findByLoanAccountIdAndInstallmentNumber(@Param("loanAccountId") UUID loanAccountId,
            @Param("installmentNumber") Integer installmentNumber);

    /**
     * Find schedules by status.
     *
     * @param loanAccountId the loan account ID
     * @param status the schedule status
     * @return list of schedules with the specified status
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.status = :status
            ORDER BY ls.installmentNumber ASC
            """)
    List<LoanSchedule> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") ScheduleStatus status);

    /**
     * Find pending schedules for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of pending schedules
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.status = 'PENDING'
            ORDER BY ls.installmentNumber ASC
            """)
    List<LoanSchedule> findPendingSchedulesByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find overdue schedules for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of overdue schedules
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.isOverdue = true
            ORDER BY ls.dueDate ASC
            """)
    List<LoanSchedule> findOverdueSchedulesByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find schedules due within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of schedules due in the date range
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.dueDate BETWEEN :startDate AND :endDate
            AND ls.status = 'PENDING'
            ORDER BY ls.dueDate ASC
            """)
    Page<LoanSchedule> findSchedulesDueBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.dueDate BETWEEN :startDate AND :endDate
            AND ls.status = 'PENDING'
            ORDER BY ls.dueDate ASC
            """)
    Page<LoanSchedule> findSchedulesDueBetweenForLoanAccount(@Param("loanAccountId") UUID loanAccountId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find schedules due on a specific date.
     *
     * @param dueDate the due date
     * @param pageable pagination information
     * @return page of schedules due on the date
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.dueDate = :dueDate
            AND ls.status = 'PENDING'
            ORDER BY ls.loanAccount.id
            """)
    Page<LoanSchedule> findSchedulesDueOnDate(@Param("dueDate") LocalDate dueDate, Pageable pageable);

    /**
     * Find schedules due on a specific date (without pagination).
     *
     * @param dueDate the due date
     * @return list of schedules due on the date
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.dueDate = :dueDate
            AND ls.status = 'PENDING'
            ORDER BY ls.loanAccount.id
            """)
    List<LoanSchedule> findSchedulesDueOnDate(@Param("dueDate") LocalDate dueDate);

    /**
     * Find overdue schedules across all loans.
     *
     * @param currentDate the current date
     * @param pageable pagination information
     * @return page of overdue schedules
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.dueDate < :currentDate
            AND ls.status = 'PENDING'
            ORDER BY ls.dueDate ASC
            """)
    Page<LoanSchedule> findOverdueSchedules(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    /**
     * Find next due schedule for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the next due schedule if found
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.status = 'PENDING'
            ORDER BY ls.dueDate ASC, ls.installmentNumber ASC
            LIMIT 1
            """)
    Optional<LoanSchedule> findNextDueSchedule(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find last paid schedule for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return optional containing the last paid schedule if found
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.status = 'PAID'
            ORDER BY ls.paidDate DESC, ls.installmentNumber DESC
            LIMIT 1
            """)
    Optional<LoanSchedule> findLastPaidSchedule(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count schedules by status for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param status the schedule status
     * @return count of schedules with the specified status
     */
    @Query("""
            SELECT COUNT(ls) FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.status = :status
            """)
    long countByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") ScheduleStatus status);

    /**
     * Count overdue schedules for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of overdue schedules
     */
    @Query("""
            SELECT COUNT(ls) FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.isOverdue = true
            """)
    long countOverdueSchedulesByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum total due for pending schedules.
     *
     * @param loanAccountId the loan account ID
     * @return total amount due
     */
    @Query("""
            SELECT COALESCE(SUM(ls.totalDue - ls.principalPaid - ls.interestPaid), 0)
            FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.status = 'PENDING'
            """)
    BigDecimal sumOutstandingByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Sum overdue amounts for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return total overdue amount
     */
    @Query("""
            SELECT COALESCE(SUM(ls.totalDue - ls.principalPaid - ls.interestPaid), 0)
            FROM LoanSchedule ls
            WHERE ls.loanAccount.id = :loanAccountId
            AND ls.isOverdue = true
            """)
    BigDecimal sumOverdueByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Update schedule status.
     *
     * @param scheduleId the schedule ID
     * @param newStatus the new status
     * @param paidDate the paid date (if applicable)
     * @return number of schedules updated
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LoanSchedule ls
            SET ls.status = :newStatus, ls.paidDate = :paidDate, ls.updatedAt = :updatedAt
            WHERE ls.id = :scheduleId
            """)
    int updateScheduleStatus(@Param("scheduleId") UUID scheduleId, @Param("newStatus") ScheduleStatus newStatus,
            @Param("paidDate") LocalDate paidDate, @Param("updatedAt") Instant updatedAt);

    /**
     * Update overdue status for schedules.
     *
     * @param scheduleId the schedule ID
     * @param isOverdue overdue flag
     * @param daysPastDue days past due
     * @return number of schedules updated
     */
    @Modifying
    @Query("""
            UPDATE LoanSchedule ls
            SET ls.isOverdue = :isOverdue, ls.daysPastDue = :daysPastDue, ls.updatedAt = :updatedAt
            WHERE ls.id = :scheduleId
            """)
    int updateOverdueStatus(@Param("scheduleId") UUID scheduleId, @Param("isOverdue") Boolean isOverdue,
            @Param("daysPastDue") Integer daysPastDue, @Param("updatedAt") Instant updatedAt);

    /**
     * Update payment amounts for a schedule.
     *
     * @param scheduleId the schedule ID
     * @param principalPaid principal amount paid
     * @param interestPaid interest amount paid
     * @param feesPaid fees paid
     * @param penaltiesPaid penalties paid
     * @param outstandingBalance remaining outstanding balance
     * @return number of schedules updated
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LoanSchedule ls
            SET ls.principalPaid = :principalPaid,
                ls.interestPaid = :interestPaid,
                ls.feesPaid = :feesPaid,
                ls.penaltiesPaid = :penaltiesPaid,
                ls.outstandingBalance = :outstandingBalance,
                ls.updatedAt = :updatedAt
            WHERE ls.id = :scheduleId
            """)
    int updatePaymentAmounts(@Param("scheduleId") UUID scheduleId, @Param("principalPaid") BigDecimal principalPaid,
            @Param("interestPaid") BigDecimal interestPaid, @Param("feesPaid") BigDecimal feesPaid,
            @Param("penaltiesPaid") BigDecimal penaltiesPaid,
            @Param("outstandingBalance") BigDecimal outstandingBalance, @Param("updatedAt") Instant updatedAt);

    /**
     * Find schedules requiring overdue check.
     *
     * @param currentDate the current date
     * @return list of schedules to check
     */
    @Query("""
            SELECT ls FROM LoanSchedule ls
            WHERE ls.dueDate < :currentDate
            AND ls.status = 'PENDING'
            AND ls.isOverdue = false
            """)
    List<LoanSchedule> findSchedulesRequiringOverdueCheck(@Param("currentDate") LocalDate currentDate);
}
