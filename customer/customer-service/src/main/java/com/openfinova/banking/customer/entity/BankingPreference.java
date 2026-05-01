package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing per-customer banking preferences.
 *
 * <p>One record per customer (one-to-one). Stores non-financial personalization
 * settings: preferred branch, language, statement format, notification channels, etc.</p>
 */
@Entity
@Table(name = "banking_preferences", indexes = { @Index(name = "idx_banking_pref_customer", columnList = "customer_id"),
        @Index(name = "idx_banking_pref_branch", columnList = "preferred_branch_code") })
public class BankingPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Owning customer. One customer has exactly one preference record.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * ISO 639-1 language code for communications (e.g., "en", "fr", "ar").
     */
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";

    /**
     * Branch sort code or identifier the customer considers their home branch.
     */
    @Column(name = "preferred_branch_code", length = 20)
    private String preferredBranchCode;

    /**
     * Whether the customer wants paper statements or electronic only.
     * Values: "PAPER", "ELECTRONIC", "BOTH"
     */
    @Column(name = "statement_delivery", length = 20)
    private String statementDelivery = "ELECTRONIC";

    /**
     * Statement frequency preference.
     * Values: "MONTHLY", "QUARTERLY", "ANNUALLY", "ON_DEMAND"
     */
    @Column(name = "statement_frequency", length = 20)
    private String statementFrequency = "MONTHLY";

    /**
     * Whether the customer opts into email transaction notifications.
     */
    @Column(name = "notify_email", nullable = false)
    private boolean notifyByEmail = true;

    /**
     * Whether the customer opts into SMS transaction notifications.
     */
    @Column(name = "notify_sms", nullable = false)
    private boolean notifyBySms = false;

    /**
     * Whether the customer opts into push notifications (mobile app).
     */
    @Column(name = "notify_push", nullable = false)
    private boolean notifyByPush = false;

    /**
     * Low-balance alert threshold (in base currency). Null means no alert.
     */
    @Column(name = "low_balance_alert_threshold", precision = 19, scale = 4)
    private BigDecimal lowBalanceAlertThreshold;

    /**
     * Large-transaction alert threshold (in base currency). Null means no alert.
     */
    @Column(name = "large_transaction_alert_threshold", precision = 19, scale = 4)
    private BigDecimal largeTransactionAlertThreshold;

    /**
     * ISO 4217 currency code for the customer's preferred display currency.
     */
    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency;

    /**
     * Preferred time zone for statement dates and transaction timestamps.
     * IANA time zone ID (e.g., "Europe/London", "America/New_York").
     */
    @Column(name = "time_zone", length = 50)
    private String timeZone;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BankingPreference() {
    }

    public BankingPreference(Customer customer) {
        this.customer = customer;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getPreferredBranchCode() {
        return preferredBranchCode;
    }

    public void setPreferredBranchCode(String preferredBranchCode) {
        this.preferredBranchCode = preferredBranchCode;
    }

    public String getStatementDelivery() {
        return statementDelivery;
    }

    public void setStatementDelivery(String statementDelivery) {
        this.statementDelivery = statementDelivery;
    }

    public String getStatementFrequency() {
        return statementFrequency;
    }

    public void setStatementFrequency(String statementFrequency) {
        this.statementFrequency = statementFrequency;
    }

    public boolean isNotifyByEmail() {
        return notifyByEmail;
    }

    public void setNotifyByEmail(boolean notifyByEmail) {
        this.notifyByEmail = notifyByEmail;
    }

    public boolean isNotifyBySms() {
        return notifyBySms;
    }

    public void setNotifyBySms(boolean notifyBySms) {
        this.notifyBySms = notifyBySms;
    }

    public boolean isNotifyByPush() {
        return notifyByPush;
    }

    public void setNotifyByPush(boolean notifyByPush) {
        this.notifyByPush = notifyByPush;
    }

    public BigDecimal getLowBalanceAlertThreshold() {
        return lowBalanceAlertThreshold;
    }

    public void setLowBalanceAlertThreshold(BigDecimal lowBalanceAlertThreshold) {
        this.lowBalanceAlertThreshold = lowBalanceAlertThreshold;
    }

    public BigDecimal getLargeTransactionAlertThreshold() {
        return largeTransactionAlertThreshold;
    }

    public void setLargeTransactionAlertThreshold(BigDecimal largeTransactionAlertThreshold) {
        this.largeTransactionAlertThreshold = largeTransactionAlertThreshold;
    }

    public String getPreferredCurrency() {
        return preferredCurrency;
    }

    public void setPreferredCurrency(String preferredCurrency) {
        this.preferredCurrency = preferredCurrency;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
