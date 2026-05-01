package com.openfinova.banking.loan.api.entity;

/**
 * IFRS 9 / CECL provisioning stage classification.
 */
public enum ProvisionStage {

    /** Stage 1: Performing loans with no significant credit deterioration (12-month ECL) */
    STAGE_1_PERFORMING,

    /** Stage 2: Loans with significant increase in credit risk (lifetime ECL) */
    STAGE_2_UNDERPERFORMING,

    /** Stage 3: Credit-impaired loans (lifetime ECL with interest on net carrying amount) */
    STAGE_3_NON_PERFORMING,

    /** Specific provision for identified losses */
    SPECIFIC_PROVISION
}
