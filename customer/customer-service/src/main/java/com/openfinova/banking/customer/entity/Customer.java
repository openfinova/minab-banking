package com.openfinova.banking.customer.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.customer.api.entity.CustomerSegmentType;
import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.api.entity.Gender;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.api.entity.LegalEntityType;
import com.openfinova.banking.customer.api.entity.MaritalStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Root entity representing a customer (Individual or Business).
 * Manages core identity, regulatory status, and relationships to contact info.
 */
@Entity
@Table(name = "customers", indexes = { @Index(name = "idx_customers_type", columnList = "type"),
        @Index(name = "idx_customers_status", columnList = "status"),
        @Index(name = "idx_customers_tax_id", columnList = "tax_id"),
        @Index(name = "idx_customers_number", columnList = "customer_number") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "customer_number", unique = true, nullable = false, length = 20, updatable = false)
    @NotNull(message = "Customer number is required")
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Customer type is required")
    private CustomerType type;

    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Column(name = "business_name", length = 200)
    @Size(max = 200, message = "Business name must not exceed 200 characters")
    private String businessName;

    @Column(name = "tax_id", unique = true, length = 50)
    private String taxId;

    @Column(name = "date_of_birth")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Customer status is required")
    private CustomerStatus status = CustomerStatus.PROSPECT;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    @NotNull(message = "KYC status is required")
    private KYCStatus kycStatus = KYCStatus.PENDING;

    @Column(name = "nationality", length = 2)
    @Size(max = 2, message = "Nationality must be ISO 3166-1 alpha-2 (2 characters)")
    @jakarta.validation.constraints.Pattern(regexp = "[A-Z]{2}", message = "Nationality must be ISO 3166-1 alpha-2 country code")
    private String nationality;

    @Column(name = "residence_country", length = 2)
    @Size(max = 2, message = "Residence country must be ISO 3166-1 alpha-2 (2 characters)")
    @jakarta.validation.constraints.Pattern(regexp = "[A-Z]{2}", message = "Residence country must be ISO 3166-1 alpha-2 country code")
    private String residenceCountry;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<CustomerAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<ContactDetail> contactDetails = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 5)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<IdentificationDocument> identificationDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 5)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<KYCWorkflow> kycWorkflows = new ArrayList<>();

    // ===== AML / COMPLIANCE FIELDS =====

    /**
     * Politically Exposed Person flag.
     * FATF Recommendation 12: mandatory PEP screening for all customers.
     */
    @Column(name = "pep_flag", nullable = false)
    private boolean pepFlag = false;

    /**
     * Sanctions screening result flag.
     * True = customer has a confirmed or unresolved match against
     * OFAC, UN, EU, or local sanctions lists.
     */
    @Column(name = "sanction_flag", nullable = false)
    private boolean sanctionFlag = false;

    /**
     * Reason the customer was blocked. Persisted for audit trail.
     * Only populated when status = BLOCKED.
     */
    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    // ===== INDIVIDUAL-SPECIFIC FIELDS =====

    /**
     * Gender identity. Applicable to INDIVIDUAL customers only.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    /**
     * Marital status. Applicable to INDIVIDUAL customers.
     * Required for KYC in many jurisdictions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 30)
    private MaritalStatus maritalStatus;

    /**
     * City/town of birth. Required for AML identity verification.
     */
    @Column(name = "place_of_birth", length = 100)
    @Size(max = 100, message = "Place of birth must not exceed 100 characters")
    private String placeOfBirth;

    /**
     * Mother's maiden name. Used as secondary identity verification factor.
     */
    @Column(name = "mother_maiden_name", length = 100)
    @Size(max = 100, message = "Mother maiden name must not exceed 100 characters")
    private String motherMaidenName;

    /**
     * Customer's current occupation or job title.
     * Used for AML risk scoring (e.g., high-risk professions like casino operators).
     */
    @Column(name = "occupation", length = 100)
    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    /**
     * Annual income in the customer's primary currency.
     * Used for suitability assessments and AML risk profiling.
     */
    @Column(name = "annual_income", precision = 19, scale = 4)
    @DecimalMin(value = "0.00", message = "Annual income must be non-negative")
    private BigDecimal annualIncome;

    // ===== BUSINESS-SPECIFIC FIELDS =====

    /**
     * Date of incorporation. Applicable to BUSINESS and TRUST customers.
     */
    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    /**
     * Country of incorporation (ISO 3166-1 alpha-2).
     */
    @Column(name = "incorporation_country", length = 2)
    @Size(max = 2, message = "Incorporation country must be ISO 3166-1 alpha-2 (2 characters)")
    @Pattern(regexp = "[A-Z]{2}", message = "Incorporation country must be ISO 3166-1 alpha-2 country code")
    private String incorporationCountry;

    /**
     * Company registration / incorporation number.
     * Different from taxId — this is the legal entity registration number.
     */
    @Column(name = "business_registration_number", length = 50)
    @Size(max = 50, message = "Business registration number must not exceed 50 characters")
    private String businessRegistrationNumber;

    /**
     * Legal entity type for business and trust customers.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "legal_entity_type", length = 30)
    private LegalEntityType legalEntityType;

    // ===== CRM / SEGMENTATION =====

    /**
     * CRM segment driving product eligibility and service levels.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "segment", length = 30)
    private CustomerSegmentType segment = CustomerSegmentType.RETAIL;

    @OneToMany(mappedBy = "customer", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<CustomerConsent> consents = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 3)
    private List<CustomerOnboarding> onboardings = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private List<CustomerAuditLog> auditLogs = new ArrayList<>();

    /** UUID of the identity user account linked to this customer party. Managed by the identity module. */
    @Column(name = "linked_identity_user_id")
    private UUID linkedIdentityUserId;

    /** Login username of the linked identity user. Maintained for human-readable audit trails. */
    @Size(max = 80)
    @Column(name = "linked_identity_username", length = 80)
    private String linkedIdentityUsername;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Customer() {
    }

    public Customer(CustomerType type, String firstName, String lastName) {
        this.type = type;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Customer(CustomerType type, String businessName) {
        this.type = type;
        this.businessName = businessName;
    }

    // Business Logic
    public boolean isActive() {
        return CustomerStatus.ACTIVE.equals(this.status);
    }

    public boolean isIndividual() {
        return CustomerType.INDIVIDUAL.equals(this.type);
    }

    public boolean isClosed() {
        return CustomerStatus.CLOSED.equals(this.status);
    }

    public String getDisplayName() {
        if (isIndividual()) {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        } else {
            return businessName;
        }
    }

    /**
     * Activates the customer. Requires KYC to be VERIFIED.
     * @throws IllegalStateException if KYC is not verified
     */
    public void activate() {
        if (!KYCStatus.VERIFIED.equals(this.kycStatus)) {
            throw new IllegalStateException("Cannot activate customer without verified KYC status");
        }
        this.status = CustomerStatus.ACTIVE;
    }

    /**
     * Blocks the customer. Sets status to BLOCKED.
     * @param reason reason for blocking — stored in blockedReason for audit trail
     */
    public void block(String reason) {
        this.status = CustomerStatus.BLOCKED;
        this.blockedReason = reason;
    }

    /**
     * Closes the customer account. Sets status to CLOSED.
     * @throws IllegalStateException if customer is not deactivated
     */
    public void close() {
        if (CustomerStatus.ACTIVE.equals(this.status)) {
            throw new IllegalStateException(
                    "Cannot close customer while status is ACTIVE. Deactivate all accounts first.");
        }
        this.status = CustomerStatus.CLOSED;
    }

    /**
     * Marks customer as deceased. Only valid for INDIVIDUAL customers.
     * @throws IllegalStateException if customer is not an individual
     */
    public void markDeceased() {
        if (!isIndividual()) {
            throw new IllegalStateException("Only individual customers can be marked as deceased");
        }
        this.status = CustomerStatus.DECEASED;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CustomerType getType() {
        return type;
    }

    public void setType(CustomerType type) {
        this.type = type;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public KYCStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KYCStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getResidenceCountry() {
        return residenceCountry;
    }

    public void setResidenceCountry(String residenceCountry) {
        this.residenceCountry = residenceCountry;
    }

    public List<CustomerAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<CustomerAddress> addresses) {
        this.addresses = addresses;
    }

    public List<ContactDetail> getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(List<ContactDetail> contactDetails) {
        this.contactDetails = contactDetails;
    }

    public List<IdentificationDocument> getIdentificationDocuments() {
        return identificationDocuments;
    }

    public void setIdentificationDocuments(List<IdentificationDocument> identificationDocuments) {
        this.identificationDocuments = identificationDocuments;
    }

    public List<KYCWorkflow> getKycWorkflows() {
        return kycWorkflows;
    }

    public void setKycWorkflows(List<KYCWorkflow> kycWorkflows) {
        this.kycWorkflows = kycWorkflows;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS / SETTERS FOR NEW FIELDS =====

    public boolean isPepFlag() {
        return pepFlag;
    }

    public void setPepFlag(boolean pepFlag) {
        this.pepFlag = pepFlag;
    }

    public boolean isSanctionFlag() {
        return sanctionFlag;
    }

    public void setSanctionFlag(boolean sanctionFlag) {
        this.sanctionFlag = sanctionFlag;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public MaritalStatus getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(MaritalStatus maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getMotherMaidenName() {
        return motherMaidenName;
    }

    public void setMotherMaidenName(String motherMaidenName) {
        this.motherMaidenName = motherMaidenName;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public LocalDate getIncorporationDate() {
        return incorporationDate;
    }

    public void setIncorporationDate(LocalDate incorporationDate) {
        this.incorporationDate = incorporationDate;
    }

    public String getIncorporationCountry() {
        return incorporationCountry;
    }

    public void setIncorporationCountry(String incorporationCountry) {
        this.incorporationCountry = incorporationCountry;
    }

    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public void setBusinessRegistrationNumber(String businessRegistrationNumber) {
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    public LegalEntityType getLegalEntityType() {
        return legalEntityType;
    }

    public void setLegalEntityType(LegalEntityType legalEntityType) {
        this.legalEntityType = legalEntityType;
    }

    public CustomerSegmentType getSegment() {
        return segment;
    }

    public void setSegment(CustomerSegmentType segment) {
        this.segment = segment;
    }

    public List<CustomerConsent> getConsents() {
        return consents;
    }

    public void setConsents(List<CustomerConsent> consents) {
        this.consents = consents;
    }

    public List<CustomerOnboarding> getOnboardings() {
        return onboardings;
    }

    public void setOnboardings(List<CustomerOnboarding> onboardings) {
        this.onboardings = onboardings;
    }

    public List<CustomerAuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<CustomerAuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    public UUID getLinkedIdentityUserId() {
        return linkedIdentityUserId;
    }

    public void setLinkedIdentityUserId(UUID linkedIdentityUserId) {
        this.linkedIdentityUserId = linkedIdentityUserId;
    }

    public String getLinkedIdentityUsername() {
        return linkedIdentityUsername;
    }

    public void setLinkedIdentityUsername(String linkedIdentityUsername) {
        this.linkedIdentityUsername = linkedIdentityUsername;
    }
}
