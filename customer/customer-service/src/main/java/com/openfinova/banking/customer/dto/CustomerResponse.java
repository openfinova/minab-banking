package com.openfinova.banking.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.api.entity.CustomerSegmentType;
import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.api.entity.Gender;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.api.entity.LegalEntityType;
import com.openfinova.banking.customer.api.entity.MaritalStatus;

/**
 * Response DTO for customer API endpoints.
 * Excludes highly sensitive fields: motherMaidenName, taxId (use tax-id lookup with customer:pii:read).
 */
public class CustomerResponse {

    private UUID id;
    private String customerNumber;
    private CustomerType type;
    private String firstName;
    private String lastName;
    private String businessName;
    private LocalDate dateOfBirth;
    private CustomerStatus status;
    private KYCStatus kycStatus;
    private String nationality;
    private String residenceCountry;
    private CustomerSegmentType segment;
    private Gender gender;
    private MaritalStatus maritalStatus;
    private String placeOfBirth;
    private String occupation;
    private BigDecimal annualIncome;
    private LocalDate incorporationDate;
    private String incorporationCountry;
    private String businessRegistrationNumber;
    private LegalEntityType legalEntityType;
    private boolean pepFlag;
    private boolean sanctionFlag;
    private UUID linkedIdentityUserId;
    private String linkedIdentityUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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

    public CustomerSegmentType getSegment() {
        return segment;
    }

    public void setSegment(CustomerSegmentType segment) {
        this.segment = segment;
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
}
