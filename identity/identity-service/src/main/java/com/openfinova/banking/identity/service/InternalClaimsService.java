package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.dto.BankingClaimsResponse;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.security.EffectiveAuthoritiesResolver;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Assembles the banking authorization snapshot consumed by Keycloak at token issuance and login.
 *
 * This is the banking-owned replacement for the claim-flattening and KYC gate that previously lived
 * in {@code TokenCustomizerConfig}. It reuses {@link EffectiveAuthoritiesResolver} for permission
 * resolution and {@link CustomerInfoService} for KYC posture, and computes a single
 * {@code eligible} flag so the Keycloak KYC authenticator can deny ineligible logins.
 */
@Service
public class InternalClaimsService {

    /** Authority that authorises trusted service-to-service customer reads (see CustomerService). */
    private static final String SERVICE_CUSTOMER_READ_AUTHORITY = "service:customer:read";

    private final UserRepository userRepository;
    private final ObjectProvider<CustomerInfoService> customerInfoServiceProvider;
    private final DateTimeService dateTimeService;
    private final boolean requireVerifiedKycForCustomers;

    public InternalClaimsService(UserRepository userRepository,
            ObjectProvider<CustomerInfoService> customerInfoServiceProvider, DateTimeService dateTimeService,
            @Value("${identity.kyc.require-verified-for-customers:true}") boolean requireVerifiedKycForCustomers) {
        this.userRepository = userRepository;
        this.customerInfoServiceProvider = customerInfoServiceProvider;
        this.dateTimeService = dateTimeService;
        this.requireVerifiedKycForCustomers = requireVerifiedKycForCustomers;
    }

    /**
     * Builds the banking claims snapshot for the given banking user id.
     *
     * @param userId the persistent {@code identity_users} id (equals the JWT {@code sub})
     * @return the assembled claims, including the {@code eligible} login gate
     * @throws ResourceNotFoundException when no banking user exists for the id
     */
    @Transactional(readOnly = true)
    public BankingClaimsResponse buildClaims(UUID userId) {
        BankingUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> permissions = EffectiveAuthoritiesResolver
                .resolveEffectivePermissions(user, dateTimeService.clock()).stream()
                .map(BankingPermission::getAuthority).sorted().toList();

        UserType userType = user.getUserType();
        UUID partyId = user.getCustomerPartyId();

        String kycStatus = null;
        String denyReason = accountStateDenyReason(user);

        if (denyReason == null && userType == UserType.CUSTOMER) {
            CustomerInfoService customerInfo = customerInfoServiceProvider.getIfAvailable();
            Optional<KYCStatus> kycOpt = (customerInfo != null && partyId != null)
                    ? readKycStatusAsServicePrincipal(customerInfo, partyId)
                    : Optional.empty();
            kycStatus = kycOpt.map(Enum::name).orElse(null);
            denyReason = customerKycDenyReason(partyId, customerInfo, kycOpt);
        }

        return new BankingClaimsResponse(
                user.getId().toString(),
                user.getUsername(),
                userType.name(),
                permissions,
                partyId != null ? partyId.toString() : null,
                user.getGlApprovalRole(),
                user.getBranchCode(),
                user.getEmployeeId(),
                user.isForcePasswordChange(),
                kycStatus,
                user.isMfaEnabled(),
                denyReason == null,
                denyReason);
    }

    /**
     * Returns a deny reason when the account is not in a usable state, mirroring the enabled
     * checks in {@code BankingUserDetails#isEnabled}; {@code null} means the account is usable.
     */
    private String accountStateDenyReason(BankingUser user) {
        LocalDateTime now = dateTimeService.now();
        if (!user.isEnabled() || user.getDisabledAt() != null) {
            return "account_disabled";
        }
        if (user.getProvisioningStatus() != AccountProvisioningStatus.ACTIVE) {
            return "account_not_active";
        }
        if (user.isEffectivelySuspended(now)) {
            return "account_suspended";
        }
        return null;
    }

    /**
     * Applies the customer KYC gate previously enforced at token issuance. Only consulted when
     * {@code identity.kyc.require-verified-for-customers} is enabled.
     */
    private String customerKycDenyReason(UUID partyId, CustomerInfoService customerInfo, Optional<KYCStatus> kycOpt) {
        if (!requireVerifiedKycForCustomers) {
            return null;
        }
        if (customerInfo == null) {
            return "kyc_service_unavailable";
        }
        if (partyId == null) {
            return "missing_customer_link";
        }
        if (kycOpt.isEmpty()) {
            return "no_customer_record";
        }
        if (kycOpt.get() != KYCStatus.VERIFIED) {
            return "kyc_not_verified";
        }
        return null;
    }

    /**
     * Reads customer KYC status under a trusted service principal.
     *
     * The customer module guards KYC reads with
     * {@code @PreAuthorize("hasAnyAuthority('customer:read', 'service:customer:read')")}. This
     * endpoint is invoked by Keycloak with no authenticated user in the context, so it swaps in an
     * authentication carrying {@code service:customer:read} for the duration of the read and
     * restores the original context afterwards.
     */
    private static Optional<KYCStatus> readKycStatusAsServicePrincipal(CustomerInfoService customerInfo, UUID partyId) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContext systemContext = SecurityContextHolder.createEmptyContext();
            systemContext.setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "kc-claims-issuer",
                            "N/A",
                            List.of(new SimpleGrantedAuthority(SERVICE_CUSTOMER_READ_AUTHORITY))));
            SecurityContextHolder.setContext(systemContext);
            return customerInfo.getKycStatus(partyId);
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }
}
