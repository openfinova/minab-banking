package com.openfinova.banking.loan.api.entity;

/**
 * Method of loan principal and interest repayment over time.
 */
public enum AmortizationType {

    /** Equal monthly installments (EMI) - same amount each period */
    EQUAL_INSTALLMENTS("Equal Installments", "Equal monthly installments (EMI) - same amount each period"),

    /** Equal principal payments with decreasing interest */
    EQUAL_PRINCIPAL("Equal Principal", "Equal principal payments with decreasing interest"),

    /** Small periodic payments with large final payment */
    BALLOON_PAYMENT("Balloon Payment", "Small periodic payments with large final payment"),

    /** Interest-only payments with principal due at maturity */
    BULLET_PAYMENT("Bullet Payment", "Interest-only payments with principal due at maturity"),

    /** Custom repayment schedule defined per loan */
    CUSTOM("Custom", "Custom repayment schedule defined per loan");

    private final String name;
    private final String description;

    AmortizationType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
