package com.openfinova.banking.customer.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.openfinova.banking.customer.dto.AddressRequest;
import com.openfinova.banking.customer.dto.AddressResponse;
import com.openfinova.banking.customer.dto.ContactResponse;
import com.openfinova.banking.customer.dto.CustomerCreateRequest;
import com.openfinova.banking.customer.dto.CustomerDataRetentionResponse;
import com.openfinova.banking.customer.dto.CustomerRelationshipResponse;
import com.openfinova.banking.customer.dto.CustomerResponse;
import com.openfinova.banking.customer.dto.CustomerUpdateRequest;
import com.openfinova.banking.customer.dto.DataSubjectRequestResponse;
import com.openfinova.banking.customer.dto.IdentificationDocumentResponse;
import com.openfinova.banking.customer.entity.ContactDetail;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.CustomerAddress;
import com.openfinova.banking.customer.entity.CustomerDataRetention;
import com.openfinova.banking.customer.entity.CustomerRelationship;
import com.openfinova.banking.customer.entity.DataSubjectRequest;
import com.openfinova.banking.customer.entity.IdentificationDocument;

/**
 * Mapper for converting entities to response DTOs.
 * Excludes sensitive fields (motherMaidenName, pepFlag, sanctionFlag) from Customer.
 */
public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerResponse dto = new CustomerResponse();
        dto.setId(customer.getId());
        dto.setCustomerNumber(customer.getCustomerNumber());
        dto.setType(customer.getType());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setBusinessName(customer.getBusinessName());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setStatus(customer.getStatus());
        dto.setKycStatus(customer.getKycStatus());
        dto.setNationality(customer.getNationality());
        dto.setResidenceCountry(customer.getResidenceCountry());
        dto.setSegment(customer.getSegment());
        dto.setGender(customer.getGender());
        dto.setMaritalStatus(customer.getMaritalStatus());
        dto.setPlaceOfBirth(customer.getPlaceOfBirth());
        dto.setOccupation(customer.getOccupation());
        dto.setAnnualIncome(customer.getAnnualIncome());
        dto.setIncorporationDate(customer.getIncorporationDate());
        dto.setIncorporationCountry(customer.getIncorporationCountry());
        dto.setBusinessRegistrationNumber(customer.getBusinessRegistrationNumber());
        dto.setLegalEntityType(customer.getLegalEntityType());
        dto.setPepFlag(customer.isPepFlag());
        dto.setSanctionFlag(customer.isSanctionFlag());
        dto.setLinkedIdentityUserId(customer.getLinkedIdentityUserId());
        dto.setLinkedIdentityUsername(customer.getLinkedIdentityUsername());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        return dto;
    }

    public static AddressResponse toAddressResponse(CustomerAddress address) {
        if (address == null) {
            return null;
        }
        AddressResponse dto = new AddressResponse();
        dto.setId(address.getId());
        dto.setCustomerId(address.getCustomer() != null ? address.getCustomer().getId() : null);
        dto.setType(address.getType());
        dto.setLine1(address.getLine1());
        dto.setLine2(address.getLine2());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        dto.setPrimary(address.isPrimary());
        dto.setValidFrom(address.getValidFrom());
        dto.setValidTo(address.getValidTo());
        return dto;
    }

    public static ContactResponse toContactResponse(ContactDetail contact) {
        if (contact == null) {
            return null;
        }
        ContactResponse dto = new ContactResponse();
        dto.setId(contact.getId());
        dto.setCustomerId(contact.getCustomer() != null ? contact.getCustomer().getId() : null);
        dto.setType(contact.getType());
        dto.setValue(contact.getValue());
        dto.setPrimary(contact.isPrimary());
        dto.setVerified(contact.isVerified());
        dto.setVerifiedAt(contact.getVerifiedAt());
        dto.setVerifiedBy(contact.getVerifiedBy());
        return dto;
    }

    public static IdentificationDocumentResponse toDocumentResponse(IdentificationDocument doc) {
        if (doc == null) {
            return null;
        }
        IdentificationDocumentResponse dto = new IdentificationDocumentResponse();
        dto.setId(doc.getId());
        dto.setCustomerId(doc.getCustomer() != null ? doc.getCustomer().getId() : null);
        dto.setType(doc.getType());
        dto.setMaskedDocumentNumber(IdentificationDocumentResponse.maskDocumentNumber(doc.getDocumentNumber()));
        dto.setIssuingCountry(doc.getIssuingCountry());
        dto.setIssuingAuthority(doc.getIssuingAuthority());
        dto.setIssueDate(doc.getIssueDate());
        dto.setExpiryDate(doc.getExpiryDate());
        dto.setVerified(doc.isVerified());
        dto.setDocumentStatus(doc.getDocumentStatus());
        dto.setVerifiedAt(doc.getVerifiedAt());
        dto.setVerifiedBy(doc.getVerifiedBy());
        return dto;
    }

    public static List<AddressResponse> toAddressResponseList(List<CustomerAddress> addresses) {
        if (addresses == null) {
            return List.of();
        }
        return addresses.stream().map(CustomerMapper::toAddressResponse).collect(Collectors.toList());
    }

    public static List<ContactResponse> toContactResponseList(List<ContactDetail> contacts) {
        if (contacts == null) {
            return List.of();
        }
        return contacts.stream().map(CustomerMapper::toContactResponse).collect(Collectors.toList());
    }

    public static List<IdentificationDocumentResponse> toDocumentResponseList(List<IdentificationDocument> docs) {
        if (docs == null) {
            return List.of();
        }
        return docs.stream().map(CustomerMapper::toDocumentResponse).collect(Collectors.toList());
    }

    public static Customer toCustomerEntity(CustomerCreateRequest request) {
        if (request == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setCustomerNumber(request.getCustomerNumber());
        customer.setType(request.getType());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setBusinessName(request.getBusinessName());
        customer.setTaxId(request.getTaxId());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus(request.getStatus());
        customer.setKycStatus(request.getKycStatus());
        customer.setNationality(request.getNationality());
        customer.setResidenceCountry(request.getResidenceCountry());
        customer.setSegment(request.getSegment());
        customer.setGender(request.getGender());
        customer.setMaritalStatus(request.getMaritalStatus());
        customer.setPlaceOfBirth(request.getPlaceOfBirth());
        customer.setOccupation(request.getOccupation());
        customer.setAnnualIncome(request.getAnnualIncome());
        customer.setIncorporationDate(request.getIncorporationDate());
        customer.setIncorporationCountry(request.getIncorporationCountry());
        customer.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        customer.setLegalEntityType(request.getLegalEntityType());
        return customer;
    }

    public static Customer toCustomerEntity(CustomerUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setBusinessName(request.getBusinessName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setTaxId(request.getTaxId());
        customer.setNationality(request.getNationality());
        customer.setResidenceCountry(request.getResidenceCountry());
        customer.setSegment(request.getSegment());
        customer.setGender(request.getGender());
        customer.setMaritalStatus(request.getMaritalStatus());
        customer.setPlaceOfBirth(request.getPlaceOfBirth());
        customer.setMotherMaidenName(request.getMotherMaidenName());
        customer.setOccupation(request.getOccupation());
        customer.setAnnualIncome(request.getAnnualIncome());
        customer.setIncorporationDate(request.getIncorporationDate());
        customer.setIncorporationCountry(request.getIncorporationCountry());
        customer.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        customer.setLegalEntityType(request.getLegalEntityType());
        return customer;
    }

    public static CustomerAddress toAddressEntity(AddressRequest request) {
        if (request == null) {
            return null;
        }
        CustomerAddress address = new CustomerAddress();
        address.setType(request.getType());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPrimary(request.isPrimary());
        address.setValidFrom(request.getValidFrom());
        address.setValidTo(request.getValidTo());
        return address;
    }

    public static CustomerRelationshipResponse toRelationshipResponse(CustomerRelationship relationship) {
        if (relationship == null) {
            return null;
        }
        CustomerRelationshipResponse dto = new CustomerRelationshipResponse();
        dto.setId(relationship.getId());
        dto.setPrimaryCustomerId(
                relationship.getPrimaryCustomer() != null ? relationship.getPrimaryCustomer().getId() : null);
        dto.setRelatedCustomerId(
                relationship.getRelatedCustomer() != null ? relationship.getRelatedCustomer().getId() : null);
        dto.setRelationshipType(relationship.getRelationshipType());
        dto.setActive(relationship.isActive());
        dto.setCreatedBy(relationship.getCreatedBy());
        dto.setCreatedAt(relationship.getCreatedAt());
        dto.setRemovedAt(relationship.getRemovedAt());
        dto.setRemovedBy(relationship.getRemovedBy());
        dto.setNotes(relationship.getNotes());
        dto.setUpdatedAt(relationship.getUpdatedAt());
        return dto;
    }

    public static List<CustomerRelationshipResponse> toRelationshipResponseList(
            List<CustomerRelationship> relationships) {
        if (relationships == null) {
            return List.of();
        }
        return relationships.stream().map(CustomerMapper::toRelationshipResponse).collect(Collectors.toList());
    }

    public static DataSubjectRequestResponse toDataSubjectRequestResponse(DataSubjectRequest request) {
        if (request == null) {
            return null;
        }
        DataSubjectRequestResponse dto = new DataSubjectRequestResponse();
        dto.setId(request.getId());
        dto.setCustomerId(request.getCustomer() != null ? request.getCustomer().getId() : null);
        dto.setRequestType(request.getRequestType());
        dto.setStatus(request.getStatus());
        dto.setReceivedAt(request.getReceivedAt());
        dto.setDueBy(request.getDueBy());
        dto.setFulfilledAt(request.getFulfilledAt());
        dto.setChannel(request.getChannel());
        dto.setReferenceNumber(request.getReferenceNumber());
        dto.setCustomerNotes(request.getCustomerNotes());
        dto.setOutcomeReason(request.getOutcomeReason());
        dto.setDeferredUntil(request.getDeferredUntil());
        dto.setExtended(request.isExtended());
        dto.setExtensionNotifiedAt(request.getExtensionNotifiedAt());
        dto.setHandledBy(request.getHandledBy());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }

    public static List<DataSubjectRequestResponse> toDataSubjectRequestResponseList(List<DataSubjectRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(CustomerMapper::toDataSubjectRequestResponse).collect(Collectors.toList());
    }

    public static CustomerDataRetentionResponse toDataRetentionResponse(CustomerDataRetention retention) {
        if (retention == null) {
            return null;
        }
        CustomerDataRetentionResponse dto = new CustomerDataRetentionResponse();
        dto.setId(retention.getId());
        dto.setCustomerId(retention.getCustomer() != null ? retention.getCustomer().getId() : null);
        dto.setRelationshipEndedAt(retention.getRelationshipEndedAt());
        dto.setRetentionExpiresAt(retention.getRetentionExpiresAt());
        dto.setRetentionYears(retention.getRetentionYears());
        dto.setLegalBasis(retention.getLegalBasis());
        dto.setAnonymized(retention.isAnonymized());
        dto.setAnonymizedAt(retention.getAnonymizedAt());
        dto.setAnonymizedBy(retention.getAnonymizedBy());
        dto.setCreatedAt(retention.getCreatedAt());
        dto.setUpdatedAt(retention.getUpdatedAt());
        return dto;
    }
}
