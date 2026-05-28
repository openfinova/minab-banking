package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.entity.AgingBracket;
import com.openfinova.banking.gl.api.entity.EscalationLevel;
import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.api.entity.SuspenseStatus;
import com.openfinova.banking.gl.dto.ClearSuspenseRequest;
import com.openfinova.banking.gl.dto.SuspenseAgingBucketDTO;
import com.openfinova.banking.gl.dto.SuspenseAgingReportDTO;
import com.openfinova.banking.gl.dto.SuspenseItemFilterDTO;
import com.openfinova.banking.gl.dto.SuspenseItemResponse;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.entity.SuspenseClearingRule;
import com.openfinova.banking.gl.entity.SuspenseEscalation;
import com.openfinova.banking.gl.entity.SuspenseItem;
import com.openfinova.banking.gl.mapper.SuspenseMapper;
import com.openfinova.banking.gl.repository.SuspenseClearingRuleRepository;
import com.openfinova.banking.gl.repository.SuspenseEscalationRepository;
import com.openfinova.banking.gl.repository.SuspenseItemRepository;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Consolidated service for managing suspense account operations.
 *
 * BUSINESS CONTEXT:
 * Manages the complete lifecycle of suspense items from creation through
 * investigation, escalation, and eventual clearing. Implements regulatory
 * requirements for timely resolution and proper oversight.
 *
 * RESPONSIBILITIES:
 * - Create suspense items (typically called from TP module)
 * - Manual and automatic clearing workflows
 * - Aging analysis and reporting
 * - Escalation management and SLA tracking
 * - Investigation workflow support
 *
 * REGULATORY COMPLIANCE:
 * - Basel Committee: Active management of aged balances
 * - Aging thresholds: 30/60/90/120/180 day escalation
 * - AML/CFT: Enhanced due diligence for unidentified deposits
 * - Audit: Complete trail of all actions and decisions
 */
@Service
@Transactional
public class SuspenseAccountService {

    private static final Logger logger = LoggerFactory.getLogger(SuspenseAccountService.class);
    private static final String SYSTEM_USER = "SYSTEM";

    private final SuspenseItemRepository suspenseItemRepository;
    private final SuspenseClearingRuleRepository clearingRuleRepository;
    private final SuspenseEscalationRepository escalationRepository;
    private final SuspenseMapper mapper;
    private final GLTransactionService transactionService;
    private final GLAccountService accountService;
    private final OperationalGLAccountService operationalGLAccountService;
    private final DateTimeService dateTimeService;

    public SuspenseAccountService(SuspenseItemRepository suspenseItemRepository,
            SuspenseClearingRuleRepository clearingRuleRepository, SuspenseEscalationRepository escalationRepository,
            SuspenseMapper mapper, GLTransactionService transactionService, GLAccountService accountService,
            OperationalGLAccountService operationalGLAccountService, DateTimeService dateTimeService) {
        this.suspenseItemRepository = suspenseItemRepository;
        this.clearingRuleRepository = clearingRuleRepository;
        this.escalationRepository = escalationRepository;
        this.mapper = mapper;
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.operationalGLAccountService = operationalGLAccountService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Clear a suspense item to the target account.
     * Creates offsetting GL transaction to move amount from suspense to correct account.
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public SuspenseItemResponse clearSuspenseItem(UUID suspenseItemId, ClearSuspenseRequest request) {
        logger.info("Clearing suspense item {} to account {}", suspenseItemId, request.getTargetAccountId());

        SuspenseItem item = suspenseItemRepository.findById(suspenseItemId)
                .orElseThrow(() -> new IllegalArgumentException("Suspense item not found: " + suspenseItemId));

        if (!item.getStatus().isActive()) {
            throw new IllegalStateException("Suspense item is not active: " + item.getStatus());
        }

        // Fetch target account identified for clearing
        GLAccount targetAccount = accountService.getAccountById(request.getTargetAccountId()).orElseThrow(
                () -> new IllegalArgumentException("Target account not found: " + request.getTargetAccountId()));

        // Fetch the operational suspense GL account for the offsetting entry
        UUID suspenseGLAccountId = operationalGLAccountService
                .getOperationalGLAccount(OperationalGLAccountType.SUSPENSE);
        GLAccount suspenseAccount = accountService.getAccountById(suspenseGLAccountId)
                .orElseThrow(() -> new IllegalStateException("Operational SUSPENSE account not configured"));

        // Build and post the offsetting GL clearing transaction:
        //   DR target account  — moves amount to correct destination
        //   CR suspense account — removes amount from temporary holding
        String clearingRef = "SUSP-CLR-" + suspenseItemId;
        String entryDescription = String
                .format("Clear suspense item %s to account %s", suspenseItemId, targetAccount.getCode());
        String txnDescription = request.getResolutionNotes() != null
                ? entryDescription + ": " + request.getResolutionNotes()
                : entryDescription;

        GLTransaction clearingTx = new GLTransaction(clearingRef, txnDescription, dateTimeService.today());
        clearingTx.setCurrency(item.getCurrency());
        clearingTx.setSource(GLTransactionSource.SYSTEM_GENERATED);

        BigDecimal amount = item.getAmount();

        GLJournalEntry debitEntry = GLJournalEntry
                .debit(targetAccount, amount, entryDescription, dateTimeService.today());
        debitEntry.setCurrency(item.getCurrency());
        debitEntry.setBaseDebitAmount(amount);
        debitEntry.setBaseCreditAmount(BigDecimal.ZERO);

        GLJournalEntry creditEntry = GLJournalEntry
                .credit(suspenseAccount, amount, entryDescription, dateTimeService.today());
        creditEntry.setCurrency(item.getCurrency());
        creditEntry.setBaseDebitAmount(BigDecimal.ZERO);
        creditEntry.setBaseCreditAmount(amount);

        clearingTx.addGLJournalEntry(debitEntry);
        clearingTx.addGLJournalEntry(creditEntry);

        GLTransaction postedClearingTx = transactionService.postTransaction(clearingTx);

        item.setTargetAccount(targetAccount);
        item.markCleared(request.getClearedBy(), postedClearingTx, dateTimeService.today());

        SuspenseItem saved = suspenseItemRepository.save(item);
        logger.info("Cleared suspense item {} with clearing transaction {}", saved.getId(), postedClearingTx.getId());

        return mapper.toSuspenseItemResponse(saved);
    }

    /**
     * Get suspense item by ID.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public SuspenseItemResponse getSuspenseItem(UUID id) {
        SuspenseItem item = suspenseItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suspense item not found: " + id));
        return mapper.toSuspenseItemResponse(item);
    }

    /**
     * Find suspense items with filters.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public Page<SuspenseItemResponse> findSuspenseItems(SuspenseItemFilterDTO filter, Pageable pageable) {
        // TODO: Implement dynamic query based on filter criteria
        // For now, simple status filter
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            return suspenseItemRepository.findByStatus(filter.getStatuses().get(0), pageable)
                    .map(mapper::toSuspenseItemResponse);
        }

        return suspenseItemRepository.findAll(pageable).map(mapper::toSuspenseItemResponse);
    }

    /**
     * Start investigation on a suspense item.
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public SuspenseItemResponse startInvestigation(UUID suspenseItemId, String investigator) {
        SuspenseItem item = suspenseItemRepository.findById(suspenseItemId)
                .orElseThrow(() -> new IllegalArgumentException("Suspense item not found: " + suspenseItemId));

        item.startInvestigation(investigator);
        SuspenseItem saved = suspenseItemRepository.save(item);
        logger.info("Started investigation on suspense item {} by {}", suspenseItemId, investigator);

        return mapper.toSuspenseItemResponse(saved);
    }

    /**
     * Generate aging report for suspense items.
     * Groups items by aging bracket with totals.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public SuspenseAgingReportDTO generateAgingReport(String currency) {
        logger.info("Generating aging report for currency: {}", currency);

        List<SuspenseItem> activeItems = suspenseItemRepository.findAllActive().stream()
                .filter(item -> currency == null || currency.equals(item.getCurrency())).collect(Collectors.toList());

        SuspenseAgingReportDTO report = new SuspenseAgingReportDTO(currency);

        // Group by aging bracket
        Map<AgingBracket, List<SuspenseItem>> buckets = activeItems.stream()
                .collect(Collectors.groupingBy(item -> item.getAgingBracket(dateTimeService.today())));

        List<SuspenseAgingBucketDTO> agingBuckets = new ArrayList<>();
        for (AgingBracket bracket : AgingBracket.values()) {
            List<SuspenseItem> items = buckets.getOrDefault(bracket, Collections.emptyList());
            BigDecimal total = items.stream().map(SuspenseItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            agingBuckets.add(mapper.toAgingBucketDTO(bracket, items.size(), total, currency));
        }

        report.setAgingBuckets(agingBuckets);
        report.setTotalItemCount(activeItems.size());
        report.setTotalAmount(
                activeItems.stream().map(SuspenseItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        report.setItemsRequiringAMLReview((int) activeItems.stream().filter(SuspenseItem::requiresAMLReview).count());

        List<SuspenseItem> escalated = activeItems.stream().filter(item -> item.getStatus() == SuspenseStatus.ESCALATED)
                .collect(Collectors.toList());
        report.setEscalatedItemCount(escalated.size());
        report.setEscalatedTotalAmount(
                escalated.stream().map(SuspenseItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        logger.info("Aging report generated: {} items, {} total", report.getTotalItemCount(), report.getTotalAmount());
        return report;
    }

    /**
     * Get items older than specified days.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<SuspenseItemResponse> getItemsOlderThan(int days) {
        LocalDate cutoffDate = dateTimeService.today().minusDays(days);
        return suspenseItemRepository.findItemsOlderThan(cutoffDate).stream().map(mapper::toSuspenseItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get items requiring AML review.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<SuspenseItemResponse> getItemsRequiringAMLReview() {
        return suspenseItemRepository.findItemsRequiringAMLReview().stream().map(mapper::toSuspenseItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Apply automatic clearing rules to eligible suspense items.
     * Returns count of items cleared.
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public int applyAutomaticClearingRules() {
        logger.info("Applying automatic clearing rules");

        List<SuspenseItem> pendingItems = suspenseItemRepository.findByStatus(SuspenseStatus.PENDING);
        List<SuspenseClearingRule> activeRules = clearingRuleRepository.findByIsActiveTrueOrderByPriorityAsc();

        int clearedCount = 0;

        for (SuspenseItem item : pendingItems) {
            SuspenseClearingRule matchingRule = findMatchingRule(item, activeRules);

            if (matchingRule != null) {
                if (matchingRule.getRequiresApproval()) {
                    logger.info(
                            "Item {} matches rule {} but requires manual approval",
                            item.getId(),
                            matchingRule.getName());
                    continue;
                }

                try {
                    // Fetch the operational suspense account for the offsetting entry
                    UUID suspenseGLAccountId = operationalGLAccountService
                            .getOperationalGLAccount(OperationalGLAccountType.SUSPENSE);
                    GLAccount suspenseAccount = accountService.getAccountById(suspenseGLAccountId).orElseThrow(
                            () -> new IllegalStateException("Operational SUSPENSE account not configured"));

                    GLAccount ruleTargetAccount = matchingRule.getTargetAccount();
                    String autoClearRef = "SUSP-AUTO-CLR-" + item.getId();
                    String autoEntryDesc = String
                            .format("Auto-clear suspense item %s via rule '%s'", item.getId(), matchingRule.getName());

                    GLTransaction autoClearingTx = new GLTransaction(
                            autoClearRef,
                            autoEntryDesc,
                            dateTimeService.today());
                    autoClearingTx.setCurrency(item.getCurrency());
                    autoClearingTx.setSource(GLTransactionSource.SYSTEM_GENERATED);

                    BigDecimal autoAmount = item.getAmount();

                    GLJournalEntry autoDebitEntry = GLJournalEntry
                            .debit(ruleTargetAccount, autoAmount, autoEntryDesc, dateTimeService.today());
                    autoDebitEntry.setCurrency(item.getCurrency());
                    autoDebitEntry.setBaseDebitAmount(autoAmount);
                    autoDebitEntry.setBaseCreditAmount(BigDecimal.ZERO);

                    GLJournalEntry autoCreditEntry = GLJournalEntry
                            .credit(suspenseAccount, autoAmount, autoEntryDesc, dateTimeService.today());
                    autoCreditEntry.setCurrency(item.getCurrency());
                    autoCreditEntry.setBaseDebitAmount(BigDecimal.ZERO);
                    autoCreditEntry.setBaseCreditAmount(autoAmount);

                    autoClearingTx.addGLJournalEntry(autoDebitEntry);
                    autoClearingTx.addGLJournalEntry(autoCreditEntry);

                    GLTransaction postedAutoClearingTx = transactionService.postTransaction(autoClearingTx);

                    item.setTargetAccount(ruleTargetAccount);
                    item.markAutoCleared(postedAutoClearingTx, dateTimeService.today());
                    suspenseItemRepository.save(item);

                    clearedCount++;
                    logger.info("Auto-cleared item {} using rule {}", item.getId(), matchingRule.getName());
                } catch (Exception e) {
                    logger.error("Failed to auto-clear item {}: {}", item.getId(), e.getMessage());
                }
            }
        }

        logger.info("Automatic clearing completed: {} items cleared", clearedCount);
        return clearedCount;
    }

    /**
     * Find the highest priority matching rule for an item.
     */
    private SuspenseClearingRule findMatchingRule(SuspenseItem item, List<SuspenseClearingRule> rules) {
        var today = dateTimeService.today();
        return rules.stream().filter(rule -> rule.matches(item, today)).findFirst().orElse(null);
    }

    /**
     * Check escalation thresholds and create escalations for aged items.
     * Returns count of new escalations created.
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public int checkEscalationThresholds() {
        logger.info("Checking escalation thresholds");

        List<SuspenseItem> activeItems = suspenseItemRepository.findAllActive();
        int escalationCount = 0;

        for (SuspenseItem item : activeItems) {
            EscalationLevel requiredLevel = determineRequiredEscalationLevel(item);

            if (requiredLevel != null) {
                // Check if already escalated to this level
                List<SuspenseEscalation> existingEscalations = escalationRepository.findBySuspenseItemId(item.getId());

                boolean alreadyEscalated = existingEscalations.stream()
                        .anyMatch(e -> e.getEscalationLevel() == requiredLevel && !e.getIsResolved());

                if (!alreadyEscalated) {
                    createEscalation(item, requiredLevel);
                    escalationCount++;
                }
            }
        }

        logger.info("Escalation check completed: {} new escalations", escalationCount);
        return escalationCount;
    }

    /**
     * Determine required escalation level based on item age.
     */
    private EscalationLevel determineRequiredEscalationLevel(SuspenseItem item) {
        long ageDays = item.getAgeDays(dateTimeService.today());

        if (ageDays >= 180) {
            return EscalationLevel.CRITICAL_BOARD_LEVEL;
        } else if (ageDays >= 120) {
            return EscalationLevel.LEVEL_4_EXECUTIVE;
        } else if (ageDays >= 90) {
            return EscalationLevel.LEVEL_3_SENIOR_MANAGEMENT;
        } else if (ageDays >= 60) {
            return EscalationLevel.LEVEL_2_MANAGER;
        } else if (ageDays >= 30) {
            return EscalationLevel.LEVEL_1_SUPERVISOR;
        }

        return null; // No escalation needed
    }

    /**
     * Create an escalation for a suspense item.
     */
    public void createEscalation(SuspenseItem item, EscalationLevel level) {
        logger.info("Creating escalation for item {} at level {}", item.getId(), level);

        LocalDate dueDate = dateTimeService.today().plusDays(level.getTypicalAgeDays());
        String assignedTo = resolveCurrentActor();

        SuspenseEscalation escalation = new SuspenseEscalation(
                item,
                level,
                assignedTo,
                dueDate,
                dateTimeService.today());
        escalation.setEscalationNotes(
                String.format(
                        "Auto-escalated: Item aged %d days exceeds threshold for %s",
                        item.getAgeDays(dateTimeService.today()),
                        level));

        escalationRepository.save(escalation);

        // Update item status to ESCALATED
        item.escalate();
        suspenseItemRepository.save(item);

        logger.info("Created escalation {} for item {}", escalation.getId(), item.getId());
    }

    /**
     * Get all unresolved escalations.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<SuspenseEscalation> getUnresolvedEscalations() {
        return escalationRepository.findByIsResolvedFalseOrderByDueDateAsc();
    }

    /**
     * Get overdue escalations (past due date).
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public List<SuspenseEscalation> getOverdueEscalations() {
        return escalationRepository.findOverdueEscalations(dateTimeService.today());
    }

    /**
     * Resolve an escalation.
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public void resolveEscalation(UUID escalationId, String resolvedBy, String resolutionNotes) {
        SuspenseEscalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new IllegalArgumentException("Escalation not found: " + escalationId));

        escalation.resolve(resolvedBy, resolutionNotes, dateTimeService.today());
        escalationRepository.save(escalation);

        logger.info("Resolved escalation {} by {}", escalationId, resolvedBy);
    }

    /**
     * Get summary statistics for active suspense items.
     */
    @PreAuthorize("hasAuthority('gl:read')")

    public Map<String, Object> getSuspenseStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<SuspenseItem> activeItems = suspenseItemRepository.findAllActive();
        stats.put("totalActiveItems", activeItems.size());

        BigDecimal totalAmount = activeItems.stream().map(SuspenseItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalActiveAmount", totalAmount);

        long amlReviewCount = activeItems.stream().filter(SuspenseItem::requiresAMLReview).count();
        stats.put("itemsRequiringAMLReview", amlReviewCount);

        long escalatedCount = activeItems.stream().filter(item -> item.getStatus() == SuspenseStatus.ESCALATED).count();
        stats.put("escalatedItems", escalatedCount);

        return stats;
    }

    private String resolveCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            try {
                return BankingPrincipal.from(auth).username();
            } catch (IllegalArgumentException ignored) {
                // Fall through to system user for batch/unauthenticated contexts
            }
        }
        return SYSTEM_USER;
    }
}
