package com.openfinova.banking.loan.repository;

import com.openfinova.banking.loan.api.entity.DocumentStatus;
import com.openfinova.banking.loan.api.entity.DocumentType;
import com.openfinova.banking.loan.entity.LoanDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LoanDocument entities.
 */
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, UUID> {

    /**
     * Find all documents for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of documents for the loan account
     */
    @Query("""
            SELECT ld FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            ORDER BY ld.createdAt DESC
            """)
    List<LoanDocument> findByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find documents by loan account and document type.
     *
     * @param loanAccountId the loan account ID
     * @param documentType the document type
     * @return list of documents matching both criteria
     */
    @Query("""
            SELECT ld FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.documentType = :documentType
            ORDER BY ld.createdAt DESC
            """)
    List<LoanDocument> findByLoanAccountIdAndDocumentType(@Param("loanAccountId") UUID loanAccountId,
            @Param("documentType") DocumentType documentType);

    /**
     * Find documents by loan account and status.
     *
     * @param loanAccountId the loan account ID
     * @param status the document status
     * @return list of documents matching both criteria
     */
    @Query("""
            SELECT ld FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = :status
            ORDER BY ld.createdAt DESC
            """)
    List<LoanDocument> findByLoanAccountIdAndStatus(@Param("loanAccountId") UUID loanAccountId,
            @Param("status") DocumentStatus status);

    /**
     * Find documents by document type.
     *
     * @param documentType the document type
     * @param pageable pagination information
     * @return page of documents of the specified type
     */
    Page<LoanDocument> findByDocumentType(DocumentType documentType, Pageable pageable);

    /**
     * Find documents by status.
     *
     * @param status the document status
     * @param pageable pagination information
     * @return page of documents with the specified status
     */
    Page<LoanDocument> findByStatus(DocumentStatus status, Pageable pageable);

    /**
     * Find pending documents for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of pending documents
     */
    @Query("""
            SELECT ld FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = 'PENDING'
            ORDER BY ld.createdAt DESC
            """)
    List<LoanDocument> findPendingDocumentsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Find verified documents for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of verified documents
     */
    @Query("""
            SELECT ld FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = 'VERIFIED'
            ORDER BY ld.createdAt DESC
            """)
    List<LoanDocument> findVerifiedDocumentsByLoanAccount(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count documents for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return count of documents
     */
    @Query("""
            SELECT COUNT(ld) FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            """)
    long countByLoanAccountId(@Param("loanAccountId") UUID loanAccountId);

    /**
     * Count documents by type for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @param documentType the document type
     * @return count of documents
     */
    @Query("""
            SELECT COUNT(ld) FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.documentType = :documentType
            """)
    long countByLoanAccountIdAndDocumentType(@Param("loanAccountId") UUID loanAccountId,
            @Param("documentType") DocumentType documentType);

    /**
     * Check if loan account has verified documents.
     *
     * @param loanAccountId the loan account ID
     * @return true if there are verified documents
     */
    @Query("""
            SELECT COUNT(ld) > 0 FROM LoanDocument ld
            WHERE ld.loanAccount.id = :loanAccountId
            AND ld.status = 'VERIFIED'
            """)
    boolean hasVerifiedDocuments(@Param("loanAccountId") UUID loanAccountId);
}
