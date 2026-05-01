package com.openfinova.banking.gl.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;

public interface GLAccountRepository extends JpaRepository<GLAccount, UUID> {

    // Essential finder methods

    /**
     * Find GL account by code.
     *
     * @param code the unique account code
     * @return optional containing the account if found
     */
    Optional<GLAccount> findByCode(String code);

    /**
     * Find GL account by code with children eagerly loaded.
     *
     * @param code the unique account code
     * @return optional containing the account with children if found
     */
    @Query("""
            SELECT a FROM GLAccount a
            LEFT JOIN FETCH a.children
            WHERE a.code = :code
            """)
    Optional<GLAccount> findByCodeWithChildren(@Param("code") String code);

    /**
     * Find GL account by ID with parent and children eagerly loaded.
     *
     * @param id the account ID
     * @return optional containing the account with relationships if found
     */
    @Query("""
            SELECT a FROM GLAccount a
            LEFT JOIN FETCH a.parent
            LEFT JOIN FETCH a.children
            WHERE a.id = :id
            """)
    Optional<GLAccount> findByIdWithHierarchy(@Param("id") UUID id);

    /**
     * Find GL accounts by parent ID.
     *
     * @param parentId the parent account ID
     * @return list of child accounts
     */
    List<GLAccount> findByParentId(UUID parentId);

    /**
     * Find GL accounts by parent (entity reference).
     *
     * @param parent the parent account entity
     * @return list of child accounts
     */
    List<GLAccount> findByParent(GLAccount parent);

    /**
     * Find GL accounts by type and status.
     *
     * @param type the account type
     * @param status the account status
     * @return list of accounts matching both criteria
     */
    List<GLAccount> findByTypeAndStatus(GLAccountType type, GLAccountStatus status);

    /**
     * Find GL accounts by type.
     *
     * @param type the account type
     * @return list of accounts of the specified type
     */
    List<GLAccount> findByType(GLAccountType type);

    /**
     * Find GL accounts whose type is in the given collection.
     * Issues a single {@code WHERE type IN (...)} query so callers that need
     * multiple types (e.g. financial reports) avoid one round-trip per type.
     *
     * @param types the account types to include
     * @return list of accounts of any of the specified types, ordered by code
     */
    @Query("SELECT a FROM GLAccount a WHERE a.type IN :types ORDER BY a.code")
    List<GLAccount> findByTypeIn(@Param("types") Collection<GLAccountType> types);

    /**
     * Find GL accounts by status.
     *
     * @param status the account status
     * @return list of accounts with the specified status
     */
    List<GLAccount> findByStatus(GLAccountStatus status);

    // Hierarchical account queries with proper indexing

    /**
     * Find root accounts (accounts with no parent).
     *
     * @return list of root accounts
     */
    @Query("SELECT a FROM GLAccount a WHERE a.parent IS NULL ORDER BY a.code")
    List<GLAccount> findRootAccounts();

    /**
     * Find all descendants of a parent account (recursive).
     *
     * @param parentCode the parent account code
     * @return list of all descendant accounts
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE a.code LIKE CONCAT(:parentCode, '%')
            AND a.code != :parentCode
            ORDER BY a.code
            """)
    List<GLAccount> findAllDescendants(@Param("parentCode") String parentCode);

    /**
     * Find direct children of a parent account by code.
     *
     * @param parentCode the parent account code
     * @return list of direct child accounts
     */
    @Query("""
            SELECT a FROM GLAccount a
            JOIN a.parent p
            WHERE p.code = :parentCode
            ORDER BY a.code
            """)
    List<GLAccount> findDirectChildrenByParentCode(@Param("parentCode") String parentCode);

    /**
     * Find accounts at a specific hierarchy level (by code pattern).
     *
     * @param codePattern the code pattern (e.g., "1___" for 4-digit codes starting with 1)
     * @return list of accounts matching the pattern
     */
    @Query("SELECT a FROM GLAccount a WHERE a.code LIKE :codePattern ORDER BY a.code")
    List<GLAccount> findByCodePattern(@Param("codePattern") String codePattern);

    /**
     * Find the hierarchy path from root to a specific account.
     *
     * @param accountCode the target account code
     * @return list of accounts from root to target (ordered)
     */
    @Query(value = """
            WITH RECURSIVE account_hierarchy AS (
                SELECT id, code, name, parent_id, type, status, 0 as level
                FROM gl_accounts
                WHERE code = :accountCode
                UNION ALL
                SELECT p.id, p.code, p.name, p.parent_id, p.type, p.status, ah.level + 1
                FROM gl_accounts p
                INNER JOIN account_hierarchy ah ON p.id = ah.parent_id
            )
            SELECT * FROM account_hierarchy ORDER BY level DESC
            """, nativeQuery = true)
    List<GLAccount> findHierarchyPath(@Param("accountCode") String accountCode);

    // Postable account filtering methods

    /**
     * Find accounts that can accept postings (leaf accounts only).
     * Postable accounts are those without children.
     *
     * @param status optional status filter
     * @return list of postable accounts
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE a.status = :status
            AND NOT EXISTS (SELECT 1 FROM GLAccount c WHERE c.parent = a)
            """)
    List<GLAccount> findPostableAccountsByStatus(@Param("status") GLAccountStatus status);

    /**
     * Find all postable accounts (leaf accounts).
     *
     * @return list of all postable accounts
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE NOT EXISTS (SELECT 1 FROM GLAccount c WHERE c.parent = a)
            ORDER BY a.code
            """)
    List<GLAccount> findAllPostableAccounts();

    /**
     * Find postable accounts by type.
     *
     * @param type the account type
     * @return list of postable accounts of the specified type
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE a.type = :type
            AND NOT EXISTS (SELECT 1 FROM GLAccount c WHERE c.parent = a)
            ORDER BY a.code
            """)
    List<GLAccount> findPostableAccountsByType(@Param("type") GLAccountType type);

    /**
     * Find postable accounts by currency.
     *
     * @param currency the currency code
     * @return list of postable accounts in the specified currency
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE a.currency = :currency
            AND NOT EXISTS (SELECT 1 FROM GLAccount c WHERE c.parent = a)
            ORDER BY a.code
            """)
    List<GLAccount> findPostableAccountsByCurrency(@Param("currency") String currency);

    // Chart of accounts navigation methods

    /**
     * Find accounts by type with pagination for chart of accounts display.
     *
     * @param type the account type
     * @param pageable pagination information
     * @return page of accounts of the specified type
     */
    Page<GLAccount> findByTypeOrderByCode(GLAccountType type, Pageable pageable);

    /**
     * Find accounts by currency with pagination.
     *
     * @param currency the currency code
     * @param pageable pagination information
     * @return page of accounts in the specified currency
     */
    Page<GLAccount> findByCurrencyOrderByCode(String currency, Pageable pageable);

    /**
     * Search accounts by name or code (case-insensitive).
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of accounts matching the search term
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(a.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY a.code
            """)
    Page<GLAccount> searchByNameOrCode(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Combined filter query for the chart-of-accounts browsing UI.
     * Every parameter is optional: pass {@code null} to skip that filter.
     *
     * @param type       account type filter, {@code null} = any
     * @param status     account status filter, {@code null} = any
     * @param currency   currency code filter, {@code null} = any
     * @param searchTerm case-insensitive substring match on name or code, {@code null} = any
     * @param pageable   pagination and sort
     * @return page of matching accounts
     */
    @Query("""
            SELECT a FROM GLAccount a
            WHERE (:type     IS NULL OR a.type     = :type)
            AND   (:status   IS NULL OR a.status   = :status)
            AND   (:currency IS NULL OR a.currency = :currency)
            AND   (:searchTerm IS NULL
                   OR LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                   OR LOWER(a.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            """)
    Page<GLAccount> filterAccounts(@Param("type") GLAccountType type, @Param("status") GLAccountStatus status,
            @Param("currency") String currency, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find accounts created by a specific user.
     *
     * @param createdBy the user who created the accounts
     * @param pageable pagination information
     * @return page of accounts created by the user
     */
    Page<GLAccount> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    // Summary and reporting methods

    /**
     * Count accounts by type.
     *
     * @param type the account type
     * @return count of accounts of the specified type
     */
    long countByType(GLAccountType type);

    /**
     * Count accounts by status.
     *
     * @param status the account status
     * @return count of accounts with the specified status
     */
    long countByStatus(GLAccountStatus status);

    /**
     * Count postable accounts by type.
     *
     * @param type the account type
     * @return count of postable accounts of the specified type
     */
    @Query("""
            SELECT COUNT(a) FROM GLAccount a
            WHERE a.type = :type
            AND NOT EXISTS (SELECT 1 FROM GLAccount c WHERE c.parent = a)
            """)
    long countPostableAccountsByType(@Param("type") GLAccountType type);

    /**
     * Find accounts by code prefix (for account numbering schemes).
     *
     * @param codePrefix the code prefix
     * @return list of accounts with codes starting with the prefix
     */
    @Query("SELECT a FROM GLAccount a WHERE a.code LIKE CONCAT(:codePrefix, '%') ORDER BY a.code")
    List<GLAccount> findByCodePrefix(@Param("codePrefix") String codePrefix);

    /**
     * Check if an account code exists.
     *
     * @param code the account code to check
     * @return true if the code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Find the next available account code for a given prefix.
     *
     * @param codePrefix the code prefix
     * @return the next available code number
     */
    @Query("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(a.code, LENGTH(:codePrefix) + 1) AS int)), 0) + 1
            FROM GLAccount a
            WHERE a.code LIKE CONCAT(:codePrefix, '%')
            AND LENGTH(a.code) > LENGTH(:codePrefix)
            """)
    Integer findNextAvailableCodeNumber(@Param("codePrefix") String codePrefix);
}
