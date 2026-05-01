package com.openfinova.banking.customer.service;

import com.openfinova.banking.customer.dto.CustomerDataExport;
import com.openfinova.banking.customer.entity.*;
import com.openfinova.banking.customer.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for producing structured data exports for GDPR compliance.
 *
 * Article 15 (Right of Access) — provides a copy of all data held.
 * Article 20 (Data Portability) — exports in a machine-readable format.
 *
 * The exported DTO contains all PII and operational data but excludes
 * fields that are exempt under Art. 15(4) (adversely affecting others' rights),
 * such as internal risk scores and PEP/sanction flags.
 */
@Service
@Transactional(readOnly = true)
public class DataExportService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final IdentificationDocumentRepository documentRepository;
    private final CustomerConsentRepository consentRepository;

    public DataExportService(CustomerRepository customerRepository, CustomerAddressRepository addressRepository,
            ContactDetailRepository contactDetailRepository, IdentificationDocumentRepository documentRepository,
            CustomerConsentRepository consentRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.contactDetailRepository = contactDetailRepository;
        this.documentRepository = documentRepository;
        this.consentRepository = consentRepository;
    }

    /**
     * Build a full portable data export for the given customer.
     *
     * @param customerId          the customer to export
     * @param dataSubjectRequestId the DSAR that triggered this export (for traceability); may be null
     * @return a populated {@link CustomerDataExport} DTO
     */
    public CustomerDataExport exportCustomerData(UUID customerId, UUID dataSubjectRequestId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        CustomerDataExport export = new CustomerDataExport();
        export.setExportedAt(LocalDateTime.now());
        export.setDataSubjectRequestId(dataSubjectRequestId);

        // Core identity
        export.setCustomerId(customer.getId());
        export.setCustomerNumber(customer.getCustomerNumber());
        export.setCustomerType(customer.getType());
        export.setFirstName(customer.getFirstName());
        export.setLastName(customer.getLastName());
        export.setDateOfBirth(customer.getDateOfBirth());
        export.setNationality(customer.getNationality());
        export.setResidenceCountry(customer.getResidenceCountry());
        export.setGender(customer.getGender());
        export.setMaritalStatus(customer.getMaritalStatus());
        export.setPlaceOfBirth(customer.getPlaceOfBirth());
        export.setMotherMaidenName(customer.getMotherMaidenName());
        export.setOccupation(customer.getOccupation());
        export.setAnnualIncome(customer.getAnnualIncome());

        // Business fields
        export.setBusinessName(customer.getBusinessName());
        export.setLegalEntityType(customer.getLegalEntityType());
        export.setIncorporationDate(customer.getIncorporationDate());
        export.setIncorporationCountry(customer.getIncorporationCountry());
        export.setBusinessRegistrationNumber(customer.getBusinessRegistrationNumber());

        // Regulatory status (non-sensitive classification only)
        export.setStatus(customer.getStatus());
        export.setKycStatus(customer.getKycStatus());
        export.setSegment(customer.getSegment());
        // Note: pepFlag and sanctionFlag are intentionally excluded from
        // Art. 20 portability exports per Art. 15(4) — disclosing them could alert a
        // subject under investigation or affect third parties' rights.

        export.setAccountCreatedAt(customer.getCreatedAt());

        // Contact details
        List<ContactDetail> contacts = contactDetailRepository.findByCustomerId(customerId);
        export.setContactDetails(contacts.stream().map(c -> {
            CustomerDataExport.ContactDetailExport dto = new CustomerDataExport.ContactDetailExport();
            dto.type = c.getType();
            dto.value = c.getValue();
            dto.primary = c.isPrimary();
            dto.verified = c.isVerified();
            dto.verifiedAt = c.getVerifiedAt();
            return dto;
        }).collect(Collectors.toList()));

        // Addresses
        List<CustomerAddress> addresses = addressRepository.findByCustomerId(customerId);
        export.setAddresses(addresses.stream().map(a -> {
            CustomerDataExport.AddressExport dto = new CustomerDataExport.AddressExport();
            dto.type = a.getType();
            dto.line1 = a.getLine1();
            dto.line2 = a.getLine2();
            dto.city = a.getCity();
            dto.stateProvince = a.getState();
            dto.postalCode = a.getPostalCode();
            dto.countryCode = a.getCountry();
            dto.primary = a.isPrimary();
            dto.validFrom = a.getValidFrom();
            dto.validTo = a.getValidTo();
            return dto;
        }).collect(Collectors.toList()));

        // Identification documents (metadata only — no stored images)
        List<IdentificationDocument> docs = documentRepository.findByCustomerId(customerId);
        export.setDocuments(docs.stream().map(d -> {
            CustomerDataExport.DocumentExport dto = new CustomerDataExport.DocumentExport();
            dto.documentType = d.getType();
            dto.documentNumber = d.getDocumentNumber();
            dto.issuingCountry = d.getIssuingCountry();
            dto.issueDate = d.getIssueDate();
            dto.expiryDate = d.getExpiryDate();
            dto.documentStatus = d.getDocumentStatus();
            dto.verifiedAt = d.getVerifiedAt();
            return dto;
        }).collect(Collectors.toList()));

        // Consent history
        List<CustomerConsent> consents = consentRepository.findByCustomerIdOrderByRecordedAtDesc(customerId);
        export.setConsentRecords(consents.stream().map(con -> {
            CustomerDataExport.ConsentExport dto = new CustomerDataExport.ConsentExport();
            dto.consentType = con.getConsentType();
            dto.granted = con.isGranted();
            dto.policyVersion = con.getPolicyVersion();
            dto.captureChannel = con.getCaptureChannel();
            dto.recordedAt = con.getRecordedAt();
            dto.expiresAt = con.getExpiresAt();
            return dto;
        }).collect(Collectors.toList()));

        return export;
    }
}
