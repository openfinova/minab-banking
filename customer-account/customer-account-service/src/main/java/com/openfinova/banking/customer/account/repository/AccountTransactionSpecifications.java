package com.openfinova.banking.customer.account.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountTransaction;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Dynamic {@link Specification} for user-scoped account transaction search.
 */
public final class AccountTransactionSpecifications {

    private AccountTransactionSpecifications() {
    }

    public static Specification<AccountTransaction> byPrimaryUser(UUID userProfileId, LocalDateTime fromDate,
            LocalDateTime toDate, UUID accountId, AccountTransactionType transactionType, String status,
            String searchContains) {

        return (root, query, cb) -> {
            Join<AccountTransaction, Account> account = root.join("customerAccount", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(account.get("primaryUserProfileId"), userProfileId));
            predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), fromDate));
            predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), toDate));

            if (accountId != null) {
                predicates.add(cb.equal(account.get("id"), accountId));
            }
            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (searchContains != null && !searchContains.isBlank()) {
                String pattern = "%" + escapeForLike(searchContains.trim()) + "%";
                Predicate description = cb.and(
                        cb.isNotNull(root.get("description")),
                        cb.like(cb.lower(root.get("description")), pattern.toLowerCase(), '\\'));
                Predicate reference = cb.and(
                        cb.isNotNull(root.get("referenceId")),
                        cb.like(cb.lower(root.get("referenceId")), pattern.toLowerCase(), '\\'));
                predicates.add(cb.or(description, reference));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeForLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
