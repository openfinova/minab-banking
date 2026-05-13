package com.openfinova.banking.customer.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.openfinova.banking.customer.dto.AddressResponse;
import com.openfinova.banking.customer.dto.ContactResponse;
import com.openfinova.banking.customer.dto.CustomerResponse;
import com.openfinova.banking.customer.dto.IdentificationDocumentResponse;
import com.openfinova.banking.customer.entity.ContactDetail;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.CustomerAddress;
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
}
