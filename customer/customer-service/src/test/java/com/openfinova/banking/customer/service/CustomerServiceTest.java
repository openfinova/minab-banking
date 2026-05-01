package com.openfinova.banking.customer.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.repository.CustomerRelationshipRepository;
import com.openfinova.banking.customer.repository.CustomerRepository;
import com.openfinova.banking.customer.repository.IdentificationDocumentRepository;
import com.openfinova.banking.customer.repository.KYCWorkflowRepository;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerRelationshipRepository relationshipRepository;
    @Mock
    private KYCWorkflowRepository kycWorkflowRepository;
    @Mock
    private IdentificationDocumentRepository identificationDocumentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                customerRepository,
                relationshipRepository,
                kycWorkflowRepository,
                identificationDocumentRepository,
                eventPublisher);
    }

    @Test
    void updateCustomerStatus_toActive_callsActivate() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerNumber("CUST-001");
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.PROSPECT);
        customer.setKycStatus(KYCStatus.VERIFIED); // activate() requires verified KYC

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        customerService.updateCustomerStatus(customer.getId(), CustomerStatus.ACTIVE);

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        verify(customerRepository).save(customer);
    }

    @Test
    void updateCustomerStatus_toAnonymized_throws() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerNumber("CUST-001");
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.ACTIVE);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.updateCustomerStatus(customer.getId(), CustomerStatus.ANONYMIZED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ANONYMIZED status can only be set by the anonymization pipeline");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateKYCStatus_throws() {
        UUID customerId = UUID.randomUUID();

        assertThatThrownBy(() -> customerService.updateKYCStatus(customerId, KYCStatus.VERIFIED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Direct KYC status update is not allowed");

        verify(customerRepository, never()).findById(any());
    }

    @Test
    void deleteCustomer_throws() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(true);

        assertThatThrownBy(() -> customerService.deleteCustomer(customerId)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Customer deletion is not allowed");
    }
}
