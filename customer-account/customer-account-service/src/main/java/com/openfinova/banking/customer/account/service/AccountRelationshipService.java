package com.openfinova.banking.customer.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.dto.AddBeneficiaryRequest;
import com.openfinova.banking.customer.account.api.dto.ValidationResult;
import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import com.openfinova.banking.customer.account.api.entity.RelationshipStatus;
import com.openfinova.banking.customer.account.api.entity.RelationshipType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountRelationship;
import com.openfinova.banking.customer.account.repository.AccountRelationshipRepository;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of AccountRelationshipService for managing associations between users and accounts.
 *
 * This service provides functionality for:
 * - Creating and managing relationships between users and accounts
 * - Managing permissions and ownership percentages
 * - Handling beneficiary designations
 * - Validating beneficiary allocations
 */
@Service
@Transactional
public class AccountRelationshipService {

    private static final Logger logger = LoggerFactory.getLogger(AccountRelationshipService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final AccountRelationshipRepository accountRelationshipRepository;
    private final AccountRepository accountRepository;

    /**
     * Constructs a new AccountRelationshipService with required dependencies.
     *
     * @param accountRelationshipRepository the repository for managing account relationship entities
     * @param accountRepository the repository for accessing account entities
     */
    public AccountRelationshipService(AccountRelationshipRepository accountRelationshipRepository,
            AccountRepository accountRepository) {
        this.accountRelationshipRepository = accountRelationshipRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Adds a new relationship between a user profile and an account.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @param relationshipType the type of relationship to establish
     * @param createdBy the user or system establishing the relationship
     * @return the newly created account relationship entity
     * @throws EntityNotFoundException if the account is not found
     * @throws IllegalArgumentException if required parameters are missing
     */
    public AccountRelationship addRelationship(UUID accountId, UUID userProfileId, RelationshipType relationshipType,
            String createdBy) {
        logger.debug(
                "Adding relationship: accountId={}, userProfileId={}, type={}",
                accountId,
                userProfileId,
                relationshipType);

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Validate inputs
        if (userProfileId == null) {
            throw new IllegalArgumentException("User profile ID is required");
        }
        if (relationshipType == null) {
            throw new IllegalArgumentException("Relationship type is required");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("Created by is required");
        }

        // Create the relationship
        AccountRelationship relationship = new AccountRelationship(account, userProfileId, relationshipType, createdBy);
        relationship.setStatus(RelationshipStatus.ACTIVE);

        AccountRelationship savedRelationship = accountRelationshipRepository.save(relationship);

        logger.info(
                "Created relationship {} for account {} and user {}: type={}",
                savedRelationship.getId(),
                accountId,
                userProfileId,
                relationshipType);

        return savedRelationship;
    }

    /**
     * Updates the set of permissions associated with an account relationship.
     *
     * @param relationshipId the unique identifier of the relationship
     * @param permissions the new set of permissions
     * @throws EntityNotFoundException if the relationship is not found
     * @throws IllegalArgumentException if permissions are null
     */
    public void updatePermissions(UUID relationshipId, Set<AccountPermission> permissions) {
        logger.debug("Updating permissions for relationship {}: permissions={}", relationshipId, permissions);

        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));

        if (permissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }

        relationship.setPermissions(permissions);
        accountRelationshipRepository.save(relationship);

        logger.info("Updated permissions for relationship {}: new permissions={}", relationshipId, permissions);
    }

    /**
     * Updates the percentage ownership for an account relationship.
     *
     * @param relationshipId the unique identifier of the relationship
     * @param percentageOwnership the new ownership percentage (0 to 100)
     * @throws EntityNotFoundException if the relationship is not found
     * @throws IllegalArgumentException if the percentage is invalid
     */
    public void updateOwnership(UUID relationshipId, BigDecimal percentageOwnership) {
        logger.debug("Updating ownership for relationship {}: percentage={}", relationshipId, percentageOwnership);

        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));

        // Validate percentage
        if (percentageOwnership == null) {
            throw new IllegalArgumentException("Percentage ownership is required");
        }
        if (percentageOwnership.compareTo(BigDecimal.ZERO) < 0 || percentageOwnership.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Percentage ownership must be between 0 and 100");
        }

        relationship.setPercentageOwnership(percentageOwnership);
        accountRelationshipRepository.save(relationship);

        logger.info("Updated ownership for relationship {}: new percentage={}", relationshipId, percentageOwnership);
    }

    /**
     * Sets an existing relationship as a beneficiary with a specific allocation percentage.
     *
     * @param relationshipId the unique identifier of the relationship
     * @param beneficiaryPercentage the percentage allocated to the beneficiary (0.01 to 100)
     * @throws EntityNotFoundException if the relationship is not found
     * @throws IllegalArgumentException if the percentage is invalid
     */
    public void setAsBeneficiary(UUID relationshipId, BigDecimal beneficiaryPercentage) {
        logger.debug("Setting relationship {} as beneficiary: percentage={}", relationshipId, beneficiaryPercentage);

        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));

        // Validate percentage
        if (beneficiaryPercentage == null) {
            throw new IllegalArgumentException("Beneficiary percentage is required");
        }
        if (beneficiaryPercentage.compareTo(BigDecimal.ZERO) <= 0 || beneficiaryPercentage.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Beneficiary percentage must be between 0.01 and 100");
        }

        relationship.setBeneficiary(beneficiaryPercentage);
        accountRelationshipRepository.save(relationship);

        logger.info("Set relationship {} as beneficiary: percentage={}", relationshipId, beneficiaryPercentage);
    }

    /**
     * Retrieves all active relationships associated with a specific account.
     *
     * @param accountId the unique identifier of the account
     * @return a list of active account relationships
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public List<AccountRelationship> getRelationshipsByAccount(UUID accountId) {
        logger.debug("Getting relationships for account: {}", accountId);

        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        List<AccountRelationship> relationships = accountRelationshipRepository
                .findActiveRelationshipsByAccount(accountId);

        logger.debug("Found {} relationships for account {}", relationships.size(), accountId);

        return relationships;
    }

    /**
     * Retrieves all active account relationships associated with a specific user profile.
     *
     * @param userProfileId the unique identifier of the user profile
     * @return a list of active account relationships across all accounts
     * @throws IllegalArgumentException if the user profile ID is missing
     */
    @Transactional(readOnly = true)
    public List<AccountRelationship> getRelationshipsByUserProfile(UUID userProfileId) {
        logger.debug("Getting relationships for user profile: {}", userProfileId);

        if (userProfileId == null) {
            throw new IllegalArgumentException("User profile ID is required");
        }

        // Find all accounts where this user has a relationship
        List<Account> accounts = accountRepository.findAccountsForUser(userProfileId);

        // Get all relationships for this user across all accounts
        List<AccountRelationship> relationships = accounts.stream()
                .flatMap(account -> account.getRelationships().stream())
                .filter(rel -> userProfileId.equals(rel.getUserProfileId()))
                .filter(rel -> RelationshipStatus.ACTIVE.equals(rel.getStatus())).toList();

        logger.debug("Found {} relationships for user profile {}", relationships.size(), userProfileId);

        return relationships;
    }

    /**
     * Deactivates an active relationship, ending its effective period.
     *
     * @param relationshipId the unique identifier of the relationship
     * @throws EntityNotFoundException if the relationship is not found
     * @throws IllegalStateException if the relationship is already inactive
     */
    public void deactivateRelationship(UUID relationshipId) {
        logger.debug("Deactivating relationship: {}", relationshipId);

        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));

        if (relationship.getStatus() != RelationshipStatus.ACTIVE) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot deactivate relationship %s: current status is %s",
                            relationshipId,
                            relationship.getStatus()));
        }

        relationship.setStatus(RelationshipStatus.INACTIVE);
        relationship.setEffectiveUntil(LocalDateTime.now());
        accountRelationshipRepository.save(relationship);

        logger.info(
                "Deactivated relationship {} for account {} and user {}",
                relationshipId,
                relationship.getCustomerAccount().getId(),
                relationship.getUserProfileId());
    }

    /**
     * Permanently deletes an account relationship from the system.
     *
     * @param relationshipId the unique identifier of the relationship
     * @throws EntityNotFoundException if the relationship is not found
     */
    public void removeRelationship(UUID relationshipId) {
        logger.debug("Removing relationship: {}", relationshipId);

        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));

        UUID accountId = relationship.getCustomerAccount().getId();
        UUID userProfileId = relationship.getUserProfileId();

        accountRelationshipRepository.delete(relationship);

        logger.warn(
                "Permanently removed relationship {} for account {} and user {}",
                relationshipId,
                accountId,
                userProfileId);
    }

    /**
     * Removes beneficiary status from an existing relationship.
     *
     * @param relationshipId the unique identifier of the relationship
     * @throws EntityNotFoundException if the relationship is not found
     */
    public void unsetAsBeneficiary(UUID relationshipId) {
        logger.debug("Unsetting beneficiary status for relationship: {}", relationshipId);

        AccountRelationship relationship = accountRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));

        relationship.removeBeneficiary();
        accountRelationshipRepository.save(relationship);

        logger.info("Removed beneficiary status from relationship {}", relationshipId);
    }

    /**
     * Adds a new beneficiary relationship to an account based on a request.
     *
     * @param accountId the unique identifier of the account
     * @param request the request containing beneficiary details
     * @return the newly created beneficiary relationship entity
     * @throws EntityNotFoundException if the account is not found
     * @throws IllegalArgumentException if required parameters are missing
     */
    public AccountRelationship addBeneficiary(UUID accountId, AddBeneficiaryRequest request) {
        logger.debug(
                "Adding beneficiary to account {}: userProfileId={}, percentage={}",
                accountId,
                request.getUserProfileId(),
                request.getPercentage());

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Validate request
        if (request.getUserProfileId() == null) {
            throw new IllegalArgumentException("User profile ID is required");
        }
        if (request.getPercentage() == null) {
            throw new IllegalArgumentException("Beneficiary percentage is required");
        }
        if (request.getCreatedBy() == null || request.getCreatedBy().isBlank()) {
            throw new IllegalArgumentException("Created by is required");
        }

        // Create beneficiary relationship
        AccountRelationship relationship = new AccountRelationship(
                account,
                request.getUserProfileId(),
                RelationshipType.BENEFICIARY,
                request.getCreatedBy());
        relationship.setStatus(RelationshipStatus.ACTIVE);
        relationship.setBeneficiary(request.getPercentage());

        // Set effective dates if provided
        if (request.getEffectiveFrom() != null) {
            relationship.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveUntil() != null) {
            relationship.setEffectiveUntil(request.getEffectiveUntil());
        }

        AccountRelationship savedRelationship = accountRelationshipRepository.save(relationship);

        logger.info(
                "Added beneficiary {} to account {}: percentage={}",
                request.getUserProfileId(),
                accountId,
                request.getPercentage());

        return savedRelationship;
    }

    /**
     * Removes beneficiary status from a specific user for an account.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @throws EntityNotFoundException if the account or active relationship is not found
     * @throws IllegalStateException if the user is not currently a beneficiary
     */
    public void removeBeneficiary(UUID accountId, UUID userProfileId) {
        logger.debug("Removing beneficiary from account {}: userProfileId={}", accountId, userProfileId);

        // Validate account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Find the beneficiary relationship
        AccountRelationship relationship = accountRelationshipRepository
                .findActiveRelationshipByAccountAndUser(accountId, userProfileId).orElseThrow(
                        () -> new EntityNotFoundException(
                                String.format(
                                        "No active relationship found for account %s and user %s",
                                        accountId,
                                        userProfileId)));

        if (!Boolean.TRUE.equals(relationship.getIsBeneficiary())) {
            throw new IllegalStateException(
                    String.format("User %s is not a beneficiary of account %s", userProfileId, accountId));
        }

        // Remove beneficiary status
        relationship.removeBeneficiary();
        accountRelationshipRepository.save(relationship);

        logger.info("Removed beneficiary status from user {} on account {}", userProfileId, accountId);
    }

    /**
     * Validates that the total beneficiary percentages for an account equal exactly 100%.
     *
     * @param accountId the unique identifier of the account
     * @return a ValidationResult indicating whether the allocations are valid
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public ValidationResult validateBeneficiaryPercentages(UUID accountId) {
        logger.debug("Validating beneficiary percentages for account: {}", accountId);

        // Validate account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        // Get all active relationships for the account
        List<AccountRelationship> relationships = accountRelationshipRepository
                .findActiveRelationshipsByAccount(accountId);

        // Calculate total beneficiary percentage
        BigDecimal totalBeneficiaryPercentage = relationships.stream()
                .filter(rel -> Boolean.TRUE.equals(rel.getIsBeneficiary()))
                .map(AccountRelationship::getBeneficiaryPercentage).filter(percentage -> percentage != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ValidationResult result = new ValidationResult();

        // Check if there are any beneficiaries
        long beneficiaryCount = relationships.stream().filter(rel -> Boolean.TRUE.equals(rel.getIsBeneficiary()))
                .count();

        if (beneficiaryCount == 0) {
            result.setValid(true);
            result.setMessage("No beneficiaries designated for this account");
            logger.debug("Account {} has no beneficiaries", accountId);
            return result;
        }

        // Validate total percentage equals 100
        if (totalBeneficiaryPercentage.compareTo(HUNDRED) == 0) {
            result.setValid(true);
            result.setMessage(
                    String.format("Beneficiary percentages are valid: total = %s%%", totalBeneficiaryPercentage));
            logger.debug(
                    "Beneficiary percentages for account {} are valid: total = {}%",
                    accountId,
                    totalBeneficiaryPercentage);
        } else if (totalBeneficiaryPercentage.compareTo(HUNDRED) < 0) {
            result.setValid(false);
            result.addError(
                    String.format(
                            "Beneficiary percentages sum to %s%%, which is less than 100%%",
                            totalBeneficiaryPercentage));
            logger.warn(
                    "Beneficiary percentages for account {} sum to {}%, less than 100%",
                    accountId,
                    totalBeneficiaryPercentage);
        } else {
            result.setValid(false);
            result.addError(
                    String.format(
                            "Beneficiary percentages sum to %s%%, which exceeds 100%%",
                            totalBeneficiaryPercentage));
            logger.warn(
                    "Beneficiary percentages for account {} sum to {}%, exceeding 100%",
                    accountId,
                    totalBeneficiaryPercentage);
        }

        return result;
    }

    /**
     * Checks whether a user profile has a specific permission on an account.
     *
     * @param accountId the unique identifier of the account
     * @param userProfileId the unique identifier of the user profile
     * @param permission the permission to check
     * @return true if the user has the permission, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID accountId, UUID userProfileId, AccountPermission permission) {
        return accountRelationshipRepository.hasPermission(accountId, userProfileId, permission.name());
    }
}
