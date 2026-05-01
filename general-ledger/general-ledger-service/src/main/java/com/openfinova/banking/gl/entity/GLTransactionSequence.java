package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a gapless transaction numbering sequence for a fiscal period.
 *
 * Each fiscal period has its own independent sequence that starts at 0 and increments
 * for each posted transaction. This ensures gapless sequential numbering within each
 * period for regulatory compliance and audit trail requirements.
 *
 * Key characteristics:
 * - One sequence per fiscal period (1:1 relationship)
 * - Pessimistic locking prevents concurrent number assignment conflicts
 * - Version field adds optimistic locking as additional safety layer
 * - Sequence cannot be decremented (only incremented)
 *
 * Banking compliance benefits:
 * - Guaranteed gapless numbering (no gaps on rollback due to app-level control)
 * - Per-period sequences simplify period-end reconciliation
 * - Immutable audit trail (numbers never reused)
 * - Fraud detection (gaps indicate data manipulation)
 */
@Entity
@Table(name = "gl_transaction_sequences", uniqueConstraints = @UniqueConstraint(name = "uk_seq_fiscal_period", columnNames = "fiscal_period_id"), indexes = {
        @Index(name = "idx_seq_fiscal_period", columnList = "fiscal_period_id") })
public class GLTransactionSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The fiscal period this sequence belongs to.
     * One sequence per period ensures independent numbering per accounting period.
     */
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    @NotNull(message = "Fiscal period is required")
    private FiscalPeriod fiscalPeriod;

    /**
     * The last assigned transaction number for this period.
     * Starts at 0 (no transactions yet), first transaction receives number 1.
     *
     * Invariant: lastAssignedNumber >= 0
     * Invariant: lastAssignedNumber = count of posted transactions in period
     */
    @Column(name = "last_assigned_number", nullable = false)
    @NotNull(message = "Last assigned number is required")
    private Long lastAssignedNumber = 0L;

    /**
     * Version field for optimistic locking.
     * Provides additional safety layer beyond pessimistic locking.
     * Incremented automatically on each update.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Timestamp of last sequence number assignment.
     * Used for audit trail and monitoring sequence usage patterns.
     */
    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // Constructors

    public GLTransactionSequence() {
    }

    /**
     * Creates a new sequence for a fiscal period.
     * Initial sequence value is 0 (first transaction will get number 1).
     *
     * @param fiscalPeriod the fiscal period this sequence belongs to
     */
    public GLTransactionSequence(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
        this.lastAssignedNumber = 0L;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public Long getLastAssignedNumber() {
        return lastAssignedNumber;
    }

    /**
     * Sets the last assigned number.
     *
     * WARNING: Should only be incremented, never decremented.
     * Use incrementAndGet() for safe number generation.
     *
     * @param lastAssignedNumber the new last assigned number (must be >= current)
     */
    public void setLastAssignedNumber(Long lastAssignedNumber) {
        this.lastAssignedNumber = lastAssignedNumber;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Increments the sequence and returns the new number.
     * This is the safe way to assign transaction numbers.
     *
     * Thread-safety: Must be called within a transaction with pessimistic lock.
     *
     * @return the next transaction number (lastAssignedNumber + 1)
     */
    public Long incrementAndGet() {
        this.lastAssignedNumber++;
        return this.lastAssignedNumber;
    }

    @Override
    public String toString() {
        return "GLTransactionSequence{" + "id=" + id + ", fiscalPeriodId="
                + (fiscalPeriod != null ? fiscalPeriod.getId() : null) + ", lastAssignedNumber=" + lastAssignedNumber
                + ", version=" + version + ", lastUpdated=" + lastUpdated + '}';
    }
}
