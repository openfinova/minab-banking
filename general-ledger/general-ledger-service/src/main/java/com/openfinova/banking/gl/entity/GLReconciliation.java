package com.openfinova.banking.gl.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.openfinova.banking.gl.api.entity.ReconciliationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tracks reconciliation of a GL transaction against an external statement
 * (e.g. Central Bank, SWIFT Nostro). Used to enforce that reconciled
 * transactions cannot be reversed; an adjusting journal entry must be used instead.
 */
@Entity
@Table(name = "gl_reconciliations", indexes = {
        @Index(name = "idx_gl_reconciliations_transaction", columnList = "transaction_id"),
        @Index(name = "idx_gl_reconciliations_status", columnList = "status"),
        @Index(name = "idx_gl_reconciliations_date", columnList = "reconciliation_date") })
public class GLReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    @NotNull(message = "Transaction is required")
    private GLTransaction transaction;

    @Column(name = "reconciliation_date", nullable = false)
    @NotNull(message = "Reconciliation date is required")
    private LocalDate reconciliationDate;

    @Column(name = "reconciled_by", nullable = false, length = 100)
    @NotNull(message = "Reconciled by is required")
    @Size(max = 100)
    private String reconciledBy;

    @Column(name = "external_reference", length = 255)
    @Size(max = 255)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private ReconciliationStatus status = ReconciliationStatus.RECONCILED;

    public GLReconciliation() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(GLTransaction transaction) {
        this.transaction = transaction;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public String getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(String reconciledBy) {
        this.reconciledBy = reconciledBy;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }
}
