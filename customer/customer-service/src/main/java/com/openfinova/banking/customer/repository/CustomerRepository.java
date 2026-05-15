package com.openfinova.banking.customer.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // Existing method
    Optional<Customer> findByTaxId(String taxId);

    /**
     * Find customer by tax ID with all relationships eagerly loaded.
     *
     * @param taxId the tax ID
     * @return optional containing the customer with relationships if found
     */
    @Query("""
            SELECT c FROM Customer c
            LEFT JOIN FETCH c.addresses
            LEFT JOIN FETCH c.contactDetails
            LEFT JOIN FETCH c.identificationDocuments
            WHERE c.taxId = :taxId
            """)
    Optional<Customer> findByTaxIdWithRelationships(@Param("taxId") String taxId);

    /**
     * Find customer by ID with all relationships eagerly loaded.
     *
     * @param id the customer ID
     * @return optional containing the customer with relationships if found
     */
    @Query("""
            SELECT c FROM Customer c
            LEFT JOIN FETCH c.addresses
            LEFT JOIN FETCH c.contactDetails
            LEFT JOIN FETCH c.identificationDocuments
            WHERE c.id = :id
            """)
    Optional<Customer> findByIdWithRelationships(@Param("id") UUID id);

    // Customer lookup by various identifiers and statuses

    /**
     * Find customers by first name and last name (for individuals).
     *
     * @param firstName the first name
     * @param lastName the last name
     * @return list of customers matching the name
     */
    List<Customer> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Find customers by business name (for businesses).
     *
     * @param businessName the business name
     * @return list of customers with matching business name
     */
    List<Customer> findByBusinessNameContainingIgnoreCase(String businessName);

    /**
     * Find customers by customer type.
     *
     * @param type the customer type
     * @param pageable pagination information
     * @return page of customers of the specified type
     */
    Page<Customer> findByType(CustomerType type, Pageable pageable);

    /**
     * Find customers by status.
     *
     * @param status the customer status
     * @param pageable pagination information
     * @return page of customers with the specified status
     */
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    /**
     * Find customers by status (non-paginated).
     *
     * @param status the customer status
     * @return list of customers with the specified status
     */
    List<Customer> findByStatus(CustomerStatus status);

    /**
     * Find customers by type and status.
     *
     * @param type the customer type
     * @param status the customer status
     * @param pageable pagination information
     * @return page of customers matching both criteria
     */
    Page<Customer> findByTypeAndStatus(CustomerType type, CustomerStatus status, Pageable pageable);

    /**
     * Find customers by date of birth.
     *
     * @param dateOfBirth the date of birth
     * @return list of customers with the specified date of birth
     */
    List<Customer> findByDateOfBirth(LocalDate dateOfBirth);

    /**
     * Find customers by date of birth range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of customers born in the date range
     */
    Page<Customer> findByDateOfBirthBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // KYC workflow support queries

    /**
     * Find customers by KYC status.
     *
     * @param kycStatus the KYC status
     * @param pageable pagination information
     * @return page of customers with the specified KYC status
     */
    Page<Customer> findByKycStatus(KYCStatus kycStatus, Pageable pageable);

    /**
     * Find customers by KYC status (non-paginated).
     *
     * @param kycStatus the KYC status
     * @return list of customers with the specified KYC status
     */
    List<Customer> findByKycStatus(KYCStatus kycStatus);

    /**
     * Find customers by status and KYC status.
     *
     * @param status the customer status
     * @param kycStatus the KYC status
     * @param pageable pagination information
     * @return page of customers matching both criteria
     */
    Page<Customer> findByStatusAndKycStatus(CustomerStatus status, KYCStatus kycStatus, Pageable pageable);

    /**
     * Find customers requiring KYC review (pending or expired).
     *
     * @param pageable pagination information
     * @return page of customers requiring KYC attention
     */
    @Query("SELECT c FROM Customer c WHERE c.kycStatus IN ('PENDING', 'EXPIRED', 'IN_REVIEW') ORDER BY c.updatedAt ASC")
    Page<Customer> findCustomersRequiringKYCReview(Pageable pageable);

    /**
     * Find customers with expired KYC that need re-verification.
     *
     * @param pageable pagination information
     * @return page of customers with expired KYC
     */
    @Query("SELECT c FROM Customer c WHERE c.kycStatus = 'EXPIRED' AND c.status = 'ACTIVE' ORDER BY c.updatedAt ASC")
    Page<Customer> findActiveCustomersWithExpiredKYC(Pageable pageable);

    /**
     * Find customers by KYC status and customer type.
     *
     * @param kycStatus the KYC status
     * @param type the customer type
     * @param pageable pagination information
     * @return page of customers matching both criteria
     */
    Page<Customer> findByKycStatusAndType(KYCStatus kycStatus, CustomerType type, Pageable pageable);

    // Compliance and regulatory reporting queries

    /**
     * Find customers created within a date range for regulatory reporting.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of customers created in the date range
     */
    @Query("SELECT c FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    Page<Customer> findCustomersCreatedBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Find customers updated within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of customers updated in the date range
     */
    @Query("SELECT c FROM Customer c WHERE c.updatedAt BETWEEN :startDate AND :endDate ORDER BY c.updatedAt DESC")
    Page<Customer> findCustomersUpdatedBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Find high-risk customers (blocked or with rejected KYC).
     *
     * @param pageable pagination information
     * @return page of high-risk customers
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'BLOCKED' OR c.kycStatus = 'REJECTED' ORDER BY c.updatedAt DESC")
    Page<Customer> findHighRiskCustomers(Pageable pageable);

    /**
     * Find customers by age range (for compliance reporting).
     *
     * @param minAge minimum age
     * @param maxAge maximum age
     * @param pageable pagination information
     * @return page of customers in the age range
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.type = 'INDIVIDUAL'
            AND c.dateOfBirth IS NOT NULL
            AND YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) BETWEEN :minAge AND :maxAge
            """)
    Page<Customer> findCustomersByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge, Pageable pageable);

    /**
     * Find business customers by registration date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination information
     * @return page of business customers registered in the date range
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.type = 'BUSINESS'
            AND c.createdAt BETWEEN :startDate AND :endDate
            ORDER BY c.createdAt DESC
            """)
    Page<Customer> findBusinessCustomersRegisteredBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Search and lookup methods

    /**
     * Full-text style lookup for operator consoles: customer number, names, business name,
     * contact values (email / phone), and exact customer UUID.
     * Non-deleted contacts only. Combines with optional status filter.
     *
     * @param q token for LIKE match on textual fields (never null; use "" only when unused)
     * @param status optional status filter
     * @param idMatch optional parsed UUID when {@code q} is a valid UUID string (matches {@code Customer#id} or
     *            {@code Customer#linkedIdentityUserId})
     */
    @Query("""
            SELECT DISTINCT c FROM Customer c
            LEFT JOIN c.contactDetails cd
            LEFT JOIN c.addresses addr
            WHERE (:status IS NULL OR c.status = :status)
            AND (
                LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(CONCAT(CONCAT(COALESCE(c.firstName, ''), ' '), COALESCE(c.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.businessName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR (
                    cd IS NOT NULL
                    AND cd.deletedAt IS NULL
                    AND LOWER(cd.value) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                OR (
                    addr IS NOT NULL
                    AND (
                        LOWER(addr.line1) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(addr.line2) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(addr.city) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(addr.state) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(addr.country) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(addr.postalCode) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                )
                OR (:idMatch IS NOT NULL AND c.id = :idMatch)
                OR (:idMatch IS NOT NULL AND c.linkedIdentityUserId = :idMatch)
            )
            """)
    Page<Customer> searchCustomers(@Param("q") String q, @Param("status") CustomerStatus status,
            @Param("idMatch") UUID idMatch, Pageable pageable);

    /**
     * Search customers by name (first name, last name, or business name).
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of customers matching the search term
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(c.businessName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            """)
    Page<Customer> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find customers with contact details containing specific information.
     *
     * @param contactValue the contact value to search for
     * @param pageable pagination information
     * @return page of customers with matching contact details
     */
    @Query("""
            SELECT DISTINCT c FROM Customer c
            JOIN c.contactDetails cd
            WHERE LOWER(cd.value) LIKE LOWER(CONCAT('%', :contactValue, '%'))
            """)
    Page<Customer> findByContactDetailsContaining(@Param("contactValue") String contactValue, Pageable pageable);

    /**
     * Find customers by address information.
     *
     * @param addressSearch the address search term
     * @param pageable pagination information
     * @return page of customers with matching address information
     */
    @Query("""
            SELECT DISTINCT c FROM Customer c
            JOIN c.addresses a
            WHERE LOWER(a.line1) LIKE LOWER(CONCAT('%', :addressSearch, '%'))
            OR LOWER(a.line2) LIKE LOWER(CONCAT('%', :addressSearch, '%'))
            OR LOWER(a.city) LIKE LOWER(CONCAT('%', :addressSearch, '%'))
            OR LOWER(a.state) LIKE LOWER(CONCAT('%', :addressSearch, '%'))
            OR LOWER(a.country) LIKE LOWER(CONCAT('%', :addressSearch, '%'))
            """)
    Page<Customer> findByAddressContaining(@Param("addressSearch") String addressSearch, Pageable pageable);

    // Performance optimization and indexing support

    /**
     * Check if a customer exists by tax ID.
     *
     * @param taxId the tax ID
     * @return true if customer exists with the tax ID
     */
    boolean existsByTaxId(String taxId);

    /**
     * Check if a customer exists by first name, last name, and date of birth.
     *
     * @param firstName the first name
     * @param lastName the last name
     * @param dateOfBirth the date of birth
     * @return true if customer exists with the specified details
     */
    boolean existsByFirstNameAndLastNameAndDateOfBirth(String firstName, String lastName, LocalDate dateOfBirth);

    /**
     * Check if a business customer exists by business name.
     *
     * @param businessName the business name
     * @return true if business customer exists with the name
     */
    boolean existsByBusinessName(String businessName);

    // Summary and reporting methods

    /**
     * Count customers by status.
     *
     * @param status the customer status
     * @return count of customers with the specified status
     */
    long countByStatus(CustomerStatus status);

    /**
     * Count customers by KYC status.
     *
     * @param kycStatus the KYC status
     * @return count of customers with the specified KYC status
     */
    long countByKycStatus(KYCStatus kycStatus);

    /**
     * Count customers by type.
     *
     * @param type the customer type
     * @return count of customers of the specified type
     */
    long countByType(CustomerType type);

    /**
     * Count customers created within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return count of customers created in the date range
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    long countCustomersCreatedBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find customers requiring immediate attention (blocked, rejected KYC, etc.).
     *
     * @param pageable pagination information
     * @return page of customers requiring attention
     */
    @Query("SELECT c FROM Customer c WHERE c.status IN ('BLOCKED', 'DECEASED') OR c.kycStatus IN ('REJECTED', 'EXPIRED') ORDER BY c.updatedAt DESC")
    Page<Customer> findCustomersRequiringAttention(Pageable pageable);

    /**
     * Find dormant customers (inactive status for extended period).
     *
     * @param cutoffDate cutoff date for dormancy check
     * @param pageable pagination information
     * @return page of dormant customers
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'INACTIVE' AND c.updatedAt <= :cutoffDate ORDER BY c.updatedAt ASC")
    Page<Customer> findDormantCustomers(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
}
