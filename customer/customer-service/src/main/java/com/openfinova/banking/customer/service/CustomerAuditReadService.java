package com.openfinova.banking.customer.service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.api.dto.CustomerAuditEventResponse;
import com.openfinova.banking.customer.entity.CustomerAuditLog;
import com.openfinova.banking.customer.repository.CustomerAuditLogRepository;
import com.openfinova.banking.customer.repository.CustomerRepository;

/**
 * Read-only access to {@link CustomerAuditLog} for staff consoles.
 */
@Service
@Transactional(readOnly = true)
public class CustomerAuditReadService {

    private static final String PII_READ = "customer:pii:read";

    private static final Set<String> PII_FIELD_PREFIXES = Set
            .of("tax", "email", "phone", "passport", "national", "mother", "birth", "address", "contact");

    private final CustomerAuditLogRepository auditLogRepository;
    private final CustomerRepository customerRepository;

    public CustomerAuditReadService(CustomerAuditLogRepository auditLogRepository,
            CustomerRepository customerRepository) {
        this.auditLogRepository = auditLogRepository;
        this.customerRepository = customerRepository;
    }

    public Page<CustomerAuditEventResponse> listAuditEvents(UUID customerId, Pageable pageable,
            Authentication authentication) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        boolean canSeePii = hasAuthority(authentication, PII_READ);
        return auditLogRepository.findByCustomerIdOrderByChangedAtDesc(customerId, pageable)
                .map(log -> toResponse(log, canSeePii));
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static CustomerAuditEventResponse toResponse(CustomerAuditLog log, boolean canSeePii) {
        CustomerAuditEventResponse r = new CustomerAuditEventResponse();
        r.setId(log.getId());
        r.setAction(log.getAction() != null ? log.getAction().name() : null);
        r.setFieldName(log.getFieldName());
        r.setChangedBy(log.getChangedBy());
        r.setChannel(log.getChannel());
        r.setChangedAt(log.getChangedAt());
        r.setCorrelationId(log.getCorrelationId());
        r.setRelatedEntityId(log.getRelatedEntityId());
        r.setRelatedEntityType(log.getRelatedEntityType());

        boolean maskValues = needsMasking(log.getFieldName()) && !canSeePii;
        r.setValueMasked(maskValues);
        if (maskValues) {
            r.setOldValue(log.getOldValue() != null ? "••••" : null);
            r.setNewValue(log.getNewValue() != null ? "••••" : null);
        } else {
            r.setOldValue(log.getOldValue());
            r.setNewValue(log.getNewValue());
        }
        return r;
    }

    private static boolean needsMasking(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String f = fieldName.toLowerCase(Locale.ROOT);
        return PII_FIELD_PREFIXES.stream().anyMatch(f::startsWith) || f.contains("tax") || f.contains("email")
                || f.contains("phone");
    }
}
