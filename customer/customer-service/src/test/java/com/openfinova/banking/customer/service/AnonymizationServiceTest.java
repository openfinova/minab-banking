package com.openfinova.banking.customer.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.CustomerAuditLog;
import com.openfinova.banking.customer.entity.CustomerDataRetention;
import com.openfinova.banking.customer.repository.ContactDetailRepository;
import com.openfinova.banking.customer.repository.CustomerAddressRepository;
import com.openfinova.banking.customer.repository.CustomerAuditLogRepository;
import com.openfinova.banking.customer.repository.CustomerDataRetentionRepository;
import com.openfinova.banking.customer.repository.CustomerRepository;
import com.openfinova.banking.customer.repository.IdentificationDocumentRepository;
import com.openfinova.banking.setup.api.DateTimeService;

@ExtendWith(MockitoExtension.class)
class AnonymizationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);
    private static final String HMAC_SECRET = "test-secret-key-for-anonymization";

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerAddressRepository addressRepository;
    @Mock
    private ContactDetailRepository contactDetailRepository;
    @Mock
    private IdentificationDocumentRepository documentRepository;
    @Mock
    private CustomerAuditLogRepository auditLogRepository;
    @Mock
    private CustomerDataRetentionRepository retentionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private DateTimeService dateTimeService;

    private AnonymizationService anonymizationService;

    @BeforeEach
    void setUp() {
        anonymizationService = new AnonymizationService(
                customerRepository,
                addressRepository,
                contactDetailRepository,
                documentRepository,
                auditLogRepository,
                retentionRepository,
                eventPublisher,
                dateTimeService,
                HMAC_SECRET);
        lenient().when(dateTimeService.today()).thenReturn(TODAY);
    }

    @Test
    void anonymizeCustomer_replacesTaxIdWithHmacToken() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerNumber("CUST-001");
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setTaxId("123456789");
        customer.setFirstName("John");
        customer.setLastName("Doe");

        CustomerDataRetention retention = new CustomerDataRetention();
        retention.setCustomer(customer);
        retention.setRetentionExpiresAt(TODAY.minusDays(1));
        retention.setAnonymized(false);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(retentionRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(retention));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findByCustomerId(customer.getId())).thenReturn(List.of());
        when(retentionRepository.save(any(CustomerDataRetention.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any(CustomerAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        anonymizationService.anonymizeCustomer(customer.getId(), "SYSTEM", "job-1");

        assertThat(customer.getTaxId()).startsWith("ANON-");
        assertThat(customer.getTaxId()).hasSizeGreaterThan(5);
        assertThat(customer.getFirstName()).isNull();
        assertThat(customer.getLastName()).isNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ANONYMIZED);
    }

    @Test
    void anonymizeCustomer_whenRetentionNotExpired_throws() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerNumber("CUST-001");
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.ACTIVE);

        CustomerDataRetention retention = new CustomerDataRetention();
        retention.setCustomer(customer);
        retention.setRetentionExpiresAt(TODAY.plusDays(30));
        retention.setAnonymized(false);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(retentionRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(retention));

        assertThatThrownBy(() -> anonymizationService.anonymizeCustomer(customer.getId(), "SYSTEM", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Retention period has not yet expired");

        verify(customerRepository, never()).save(any());
    }
}
