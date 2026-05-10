package com.openfinova.banking.identity.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.security.BankingUserDetails;
import com.openfinova.banking.identity.security.ClientIpResolver;
import com.openfinova.banking.identity.security.PasswordLifecycleEvaluator;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Injects banking-specific claims into every JWT issued by the Authorization Server.
 *
 * These claims are later read by {@link com.openfinova.banking.identity.api.BankingPrincipal} and
 * drive object-level ownership checks without additional database calls.
 *
 * <p>
 * For {@link UserType#CUSTOMER} users, KYC is resolved from {@link CustomerInfoService} (customer
 * module). When {@code identity.kyc.require-verified-for-customers} is true, tokens are not issued
 * unless KYC is {@link KYCStatus#VERIFIED}.
 *
 * Claim names are defined as constants on {@link BankingPrincipal} so that this config and the
 * consumer side stay in sync.
 */
@Configuration
public class TokenCustomizerConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> bankingTokenCustomizer(
            ObjectProvider<CustomerInfoService> customerInfoServiceProvider, OAuth2TokenPolicyProperties tokenPolicy,
            PasswordPolicyProperties passwordPolicy, UserRepository userRepository, DateTimeService dateTimeService,
            @Value("${identity.kyc.require-verified-for-customers:true}") boolean requireVerifiedKycForCustomers) {
        return context -> {
            if (!(context.getPrincipal().getPrincipal() instanceof BankingUserDetails details)) {
                return;
            }

            BankingUser freshUser = userRepository.findById(details.getUserId()).orElse(null);
            if (freshUser != null) {
                LocalDateTime now = LocalDateTime.now(dateTimeService.clock());
                boolean expired = PasswordLifecycleEvaluator.isPasswordExpired(freshUser, passwordPolicy, now);
                if (expired && !freshUser.isForcePasswordChange()) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error(
                                    OAuth2ErrorCodes.ACCESS_DENIED,
                                    "Password has expired. Use an administrative password reset or self-service recovery if available.",
                                    null));
                }
            }

            CustomerInfoService customerInfo = customerInfoServiceProvider.getIfAvailable();
            JwtClaimsSet.Builder claims = context.getClaims();

            UserType userType = details.getUserType();
            UUID partyId = details.getCustomerPartyId();

            if (userType == UserType.CUSTOMER) {
                if (requireVerifiedKycForCustomers) {
                    if (customerInfo == null) {
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(
                                        OAuth2ErrorCodes.INVALID_REQUEST,
                                        "Customer KYC enforcement is enabled but CustomerInfoService is not available",
                                        null));
                    }
                    if (partyId == null) {
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(
                                        OAuth2ErrorCodes.INVALID_REQUEST,
                                        "Customer login requires a linked customer_party_id",
                                        null));
                    }
                }

                Optional<KYCStatus> kycOpt = Optional.empty();
                if (customerInfo != null && partyId != null) {
                    kycOpt = customerInfo.getKycStatus(partyId);
                }

                if (requireVerifiedKycForCustomers) {
                    if (kycOpt.isEmpty()) {
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(
                                        OAuth2ErrorCodes.ACCESS_DENIED,
                                        "No customer record found for linked customer_party_id",
                                        null));
                    }
                    if (kycOpt.get() != KYCStatus.VERIFIED) {
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(
                                        OAuth2ErrorCodes.ACCESS_DENIED,
                                        "KYC must be VERIFIED before issuing tokens (current: " + kycOpt.get() + ")",
                                        null));
                    }
                }

                kycOpt.ifPresent(status -> claims.claim(BankingPrincipal.CLAIM_KYC_STATUS, status.name()));
            }

            claims.claim("preferred_username", details.getUsername());
            // BankingPrincipal and several APIs expect `sub` to be the persistent user id (UUID), not the login name.
            claims.subject(details.getUserId().toString());

            claims.claim(BankingPrincipal.CLAIM_USER_TYPE, details.getUserType().name());

            List<String> permissions = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
            claims.claim(BankingPrincipal.CLAIM_PERMISSIONS, permissions);

            if (details.getBranchCode() != null) {
                claims.claim(BankingPrincipal.CLAIM_BRANCH_CODE, details.getBranchCode());
            }
            if (details.getEmployeeId() != null) {
                claims.claim(BankingPrincipal.CLAIM_EMPLOYEE_ID, details.getEmployeeId());
            }
            if (details.getGlApprovalRole() != null) {
                claims.claim(BankingPrincipal.CLAIM_GL_APPROVAL_ROLE, details.getGlApprovalRole());
            }

            if (details.getCustomerPartyId() != null) {
                claims.claim(BankingPrincipal.CLAIM_CUSTOMER_PARTY, details.getCustomerPartyId().toString());
            }

            OAuth2Authorization authorization = context.getAuthorization();
            if (authorization != null) {
                claims.claim(BankingPrincipal.CLAIM_AUTHZ_ID, authorization.getId());
            }

            if (tokenPolicy.isIncludeClientIpClaim()) {
                var attrs = RequestContextHolder.getRequestAttributes();
                if (attrs instanceof ServletRequestAttributes sra && sra.getRequest() != null) {
                    String ip = ClientIpResolver.resolve(sra.getRequest());
                    if (ip != null && !ip.isBlank()) {
                        claims.claim(BankingPrincipal.CLAIM_CLIENT_IP, ip);
                    }
                }
            }

            boolean forceChange = freshUser != null ? freshUser.isForcePasswordChange()
                    : details.isForcePasswordChange();
            claims.claim(BankingPrincipal.CLAIM_FORCE_PASSWORD_CHANGE, forceChange);
        };
    }
}
