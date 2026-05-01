package com.openfinova.banking.customer.dto;

import com.openfinova.banking.customer.api.entity.AddressType;
import com.openfinova.banking.customer.api.entity.ConsentType;
import com.openfinova.banking.customer.api.entity.ContactType;
import com.openfinova.banking.customer.api.entity.CustomerSegmentType;
import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.api.entity.DocumentStatus;
import com.openfinova.banking.customer.api.entity.DocumentType;
import com.openfinova.banking.customer.api.entity.Gender;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.api.entity.LegalEntityType;
import com.openfinova.banking.customer.api.entity.MaritalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object used for GDPR Article 20 data portability exports.
 *
 * <p>Contains all personal data held about a customer in a structured,
 * commonly-used, and machine-readable format (JSON/CSV) so the customer
 * can take their data to another controller.</p>
 *
 * <p>Also used for GDPR Article 15 right-of-access responses.</p>
 */
public class CustomerDataExport {

    /** Export timestamp (UTC). */
    private LocalDateTime exportedAt;

    /** Version of this export schema (for forward-compatibility). */
    private String schemaVersion = "1.0";

    /** ID of the DataSubjectRequest that triggered this export. */
    private UUID dataSubjectRequestId;

    private UUID customerId;
    private String customerNumber;
    private CustomerType customerType;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String nationality;
    private String residenceCountry;
    private Gender gender;
    private MaritalStatus maritalStatus;
    private String placeOfBirth;
    private String motherMaidenName;
    private String occupation;
    private BigDecimal annualIncome;

    private String businessName;
    private LegalEntityType legalEntityType;
    private LocalDate incorporationDate;
    private String incorporationCountry;
    private String businessRegistrationNumber;

    private CustomerStatus status;
    private KYCStatus kycStatus;
    private CustomerSegmentType segment;
    private boolean pepFlag;
    private boolean sanctionFlag;

    private List<ContactDetailExport> contactDetails;
    private List<AddressExport> addresses;

    private List<DocumentExport> documents;

    private List<ConsentExport> consentRecords;

    private LocalDateTime accountCreatedAt;

    public static class ContactDetailExport {
        public ContactType type;
        public String value;
        public boolean primary;
        public boolean verified;
        public LocalDateTime verifiedAt;
    }

    public static class AddressExport {
        public AddressType type;
        public String line1;
        public String line2;
        public String city;
        public String stateProvince;
        public String postalCode;
        public String countryCode;
        public boolean primary;
        public LocalDate validFrom;
        public LocalDate validTo;
    }

    public static class DocumentExport {
        public DocumentType documentType;
        public String documentNumber;
        public String issuingCountry;
        public LocalDate issueDate;
        public LocalDate expiryDate;
        public DocumentStatus documentStatus;
        public LocalDateTime verifiedAt;
    }

    public static class ConsentExport {
        public ConsentType consentType;
        public boolean granted;
        public String policyVersion;
        public String captureChannel;
        public LocalDateTime recordedAt;
        public LocalDateTime expiresAt;
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public UUID getDataSubjectRequestId() {
        return dataSubjectRequestId;
    }

    public void setDataSubjectRequestId(UUID dataSubjectRequestId) {
        this.dataSubjectRequestId = dataSubjectRequestId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public LegalEntityType getLegalEntityType() {
        return legalEntityType;
    }

    public void setLegalEntityType(LegalEntityType legalEntityType) {
        this.legalEntityType = legalEntityType;
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

    public CustomerSegmentType getSegment() {
        return segment;
    }

    public void setSegment(CustomerSegmentType segment) {
        this.segment = segment;
    }

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

    public List<ContactDetailExport> getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(List<ContactDetailExport> contactDetails) {
        this.contactDetails = contactDetails;
    }

    public List<AddressExport> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AddressExport> addresses) {
        this.addresses = addresses;
    }

    public List<DocumentExport> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentExport> documents) {
        this.documents = documents;
    }

    public List<ConsentExport> getConsentRecords() {
        return consentRecords;
    }

    public void setConsentRecords(List<ConsentExport> consentRecords) {
        this.consentRecords = consentRecords;
    }

    public LocalDateTime getAccountCreatedAt() {
        return accountCreatedAt;
    }

    public void setAccountCreatedAt(LocalDateTime accountCreatedAt) {
        this.accountCreatedAt = accountCreatedAt;
    }
}
