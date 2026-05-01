package com.openfinova.banking.customer.repository;

import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;
import com.openfinova.banking.customer.entity.CustomerRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CustomerRelationship entity operations.
 */
@Repository
public interface CustomerRelationshipRepository extends JpaRepository<CustomerRelationship, UUID> {

    /**
     * Finds all active relationships for a customer (as primary or related).
     *
     * @param customerId the customer ID
     * @return list of customer relationships
     */
    @Query("""
            SELECT cr FROM CustomerRelationship cr WHERE cr.active = true AND
            (cr.primaryCustomer.id = :customerId OR cr.relatedCustomer.id = :customerId)
            """)
    List<CustomerRelationship> findActiveRelationshipsByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Finds all relationships where the customer is the primary customer.
     *
     * @param primaryCustomerId the primary customer ID
     * @return list of customer relationships
     */
    List<CustomerRelationship> findByPrimaryCustomerIdAndActiveTrue(UUID primaryCustomerId);

    /**
     * Finds all relationships where the customer is the related customer.
     *
     * @param relatedCustomerId the related customer ID
     * @return list of customer relationships
     */
    List<CustomerRelationship> findByRelatedCustomerIdAndActiveTrue(UUID relatedCustomerId);

    /**
     * Finds relationships by type for a customer.
     *
     * @param customerId the customer ID
     * @param relationshipType the relationship type
     * @return list of customer relationships
     */
    @Query("""
            SELECT cr FROM CustomerRelationship cr WHERE cr.active = true AND
            cr.relationshipType = :relationshipType AND
            (cr.primaryCustomer.id = :customerId OR cr.relatedCustomer.id = :customerId)
            """)
    List<CustomerRelationship> findByCustomerIdAndRelationshipType(@Param("customerId") UUID customerId,
            @Param("relationshipType") CustomerRelationshipType relationshipType);

    /**
     * Checks if a relationship already exists between two customers.
     *
     * @param primaryCustomerId the primary customer ID
     * @param relatedCustomerId the related customer ID
     * @param relationshipType the relationship type
     * @return true if relationship exists
     */
    @Query("""
            SELECT COUNT(cr) > 0 FROM CustomerRelationship cr WHERE cr.active = true AND
            ((cr.primaryCustomer.id = :primaryCustomerId AND cr.relatedCustomer.id = :relatedCustomerId) OR
            (cr.primaryCustomer.id = :relatedCustomerId AND cr.relatedCustomer.id = :primaryCustomerId)) AND
            cr.relationshipType = :relationshipType
            """)
    boolean existsActiveRelationship(@Param("primaryCustomerId") UUID primaryCustomerId,
            @Param("relatedCustomerId") UUID relatedCustomerId,
            @Param("relationshipType") CustomerRelationshipType relationshipType);
}
