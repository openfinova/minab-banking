package com.openfinova.banking.tp.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionRequest;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Dynamic {@link Specification} for paginated admin transaction search.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> adminSearch(UUID accountId, TransactionStatus status,
            TransactionType transactionType, LocalDate fromTransactionDate, LocalDate toTransactionDate,
            String currency, BigDecimal minAmount, BigDecimal maxAmount, String referenceContains) {

        return (root, query, cb) -> {
            Join<Transaction, TransactionRequest> req = root.join("request", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            if (accountId != null) {
                predicates.add(
                        cb.or(
                                cb.equal(root.get("sourceAccountId"), accountId),
                                cb.equal(root.get("destinationAccountId"), accountId)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (transactionType != null) {
                predicates.add(cb.equal(req.get("transactionType"), transactionType));
            }
            if (fromTransactionDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), fromTransactionDate));
            }
            if (toTransactionDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), toTransactionDate));
            }
            if (currency != null && !currency.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("currency")), currency.trim().toUpperCase()));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(req.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(req.get("amount"), maxAmount));
            }
            if (referenceContains != null && !referenceContains.isBlank()) {
                String pattern = "%" + escapeForLike(referenceContains.trim()) + "%";
                Predicate idem = cb.like(cb.lower(req.get("idempotencyKey")), pattern.toLowerCase(), '\\');
                Predicate ext = cb.and(
                        cb.isNotNull(root.get("externalReference")),
                        cb.like(cb.lower(root.get("externalReference")), pattern.toLowerCase(), '\\'));
                Predicate gtw = cb.and(
                        cb.isNotNull(root.get("gatewayTransactionId")),
                        cb.like(cb.lower(root.get("gatewayTransactionId")), pattern.toLowerCase(), '\\'));
                Predicate clientRef = cb.and(
                        cb.isNotNull(req.get("clientReference")),
                        cb.like(cb.lower(req.get("clientReference")), pattern.toLowerCase(), '\\'));
                predicates.add(cb.or(idem, ext, gtw, clientRef));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeForLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
