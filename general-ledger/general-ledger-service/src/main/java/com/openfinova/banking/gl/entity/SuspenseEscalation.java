package com.openfinova.banking.gl.entity;

import com.openfinova.banking.gl.api.entity.EscalationLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tracks escalations for suspense items that exceed aging thresholds.
 *
 * BUSINESS CONTEXT:
 * Ensures timely resolution of suspense items through progressive escalation
 * to higher authority levels as items age. Compliance with regulatory
 * requirements for active management and oversight.
 *
 * ESCALATION WORKFLOW:
 * - Day 30: Escalate to Supervisor (Level 1)
 * - Day 60: Escalate to Manager (Level 2)
 * - Day 90: Escalate to Senior Management (Level 3)
 * - Day 120: Escalate to Executive (Level 4)
 * - Day 180: Escalate to Board/Critical (Level 5)
 *
 * REGULATORY REQUIREMENTS:
 * - Basel Committee: Aged suspense items indicate control weaknesses
 * - Audit expectation: Executive awareness of items over 90 days
 * - Board reporting: Items over 180 days require Board-level attention
 * - SOX: Material items require documented escalation and resolution
 *
 * SLA MANAGEMENT:
 * Each escalation has a due date. Missed due dates trigger further escalation
 * or regulatory reporting obligations.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "gl_suspense_escalations", indexes = {
        @Index(name = "idx_escalation_suspense", columnList = "suspense_item_id"),
        @Index(name = "idx_escalation_level", columnList = "escalation_level"),
        @Index(name = "idx_escalation_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_escalation_due_date", columnList = "due_date"),
        @Index(name = "idx_escalation_resolved", columnList = "is_resolved") })
public class SuspenseEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The suspense item being escalated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspense_item_id", nullable = false)
    @NotNull(message = "Suspense item is required")
    private SuspenseItem suspenseItem;

    /**
     * Escalation level (supervisor, manager, senior management, executive, board).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 50)
    @NotNull(message = "Escalation level is required")
    private EscalationLevel escalationLevel;

    /**
     * When this escalation was created.
     */
    @Column(name = "escalated_date", nullable = false)
    @NotNull(message = "Escalated date is required")
    private LocalDate escalatedDate;

    /**
     * Who this escalation was assigned to.
     */
    @Column(name = "assigned_to", nullable = false, length = 100)
    @NotBlank(message = "Assigned to is required")
    @Size(max = 100, message = "Assigned to must not exceed 100 characters")
    private String assignedTo;

    /**
     * When this escalation is due for resolution.
     */
    @Column(name = "due_date", nullable = false)
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;

    /**
     * Has this escalation been resolved?
     */
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    /**
     * When this escalation was resolved.
     */
    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    /**
     * Who resolved this escalation.
     */
    @Column(name = "resolved_by", length = 100)
    @Size(max = 100, message = "Resolved by must not exceed 100 characters")
    private String resolvedBy;

    /**
     * Notes on the escalation (reason, context, urgency).
     */
    @Column(name = "escalation_notes", length = 2000)
    @Size(max = 2000, message = "Escalation notes must not exceed 2000 characters")
    private String escalationNotes;

    /**
     * Resolution notes (how it was handled, outcome).
     */
    @Column(name = "resolution_notes", length = 2000)
    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    private String resolutionNotes;

    /**
     * Whether the due date was missed.
     */
    @Column(name = "sla_breached", nullable = false)
    private Boolean slaBreached = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors

    public SuspenseEscalation() {
    }

    public SuspenseEscalation(SuspenseItem suspenseItem, EscalationLevel escalationLevel, String assignedTo,
            LocalDate dueDate, LocalDate escalatedDate) {
        this.suspenseItem = suspenseItem;
        this.escalationLevel = escalationLevel;
        this.escalatedDate = escalatedDate;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
    }

    // Business Logic Methods

    /**
     * Mark this escalation as resolved.
     */
    public void resolve(String resolvedBy, String resolutionNotes, LocalDate resolvedDate) {
        this.isResolved = true;
        this.resolvedDate = resolvedDate;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
    }

    /**
     * Check if SLA has been breached (past due date and not resolved).
     */
    public boolean checkSLABreach(LocalDate evaluatedAt) {
        if (!isResolved && evaluatedAt.isAfter(dueDate)) {
            this.slaBreached = true;
            return true;
        }
        return false;
    }

    /**
     * Days until due date (negative if overdue).
     */
    public long getDaysUntilDue(LocalDate evaluatedAt) {
        return evaluatedAt.until(dueDate, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * Is this escalation overdue?
     */
    public boolean isOverdue(LocalDate evaluatedAt) {
        return !isResolved && evaluatedAt.isAfter(dueDate);
    }

    /**
     * Requires board-level attention?
     */
    public boolean requiresBoardAttention() {
        return escalationLevel == EscalationLevel.CRITICAL_BOARD_LEVEL;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SuspenseItem getSuspenseItem() {
        return suspenseItem;
    }

    public void setSuspenseItem(SuspenseItem suspenseItem) {
        this.suspenseItem = suspenseItem;
    }

    public EscalationLevel getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(EscalationLevel escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public LocalDate getEscalatedDate() {
        return escalatedDate;
    }

    public void setEscalatedDate(LocalDate escalatedDate) {
        this.escalatedDate = escalatedDate;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }

    public LocalDate getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(LocalDate resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getEscalationNotes() {
        return escalationNotes;
    }

    public void setEscalationNotes(String escalationNotes) {
        this.escalationNotes = escalationNotes;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Boolean getSlaBreached() {
        return slaBreached;
    }

    public void setSlaBreached(Boolean slaBreached) {
        this.slaBreached = slaBreached;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SuspenseEscalation that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SuspenseEscalation{" + "id=" + id + ", escalationLevel=" + escalationLevel + ", assignedTo='"
                + assignedTo + '\'' + ", dueDate=" + dueDate + ", isResolved=" + isResolved + ", slaBreached="
                + slaBreached + '}';
    }
}
