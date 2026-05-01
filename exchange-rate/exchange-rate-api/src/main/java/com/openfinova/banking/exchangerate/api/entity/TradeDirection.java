package com.openfinova.banking.exchangerate.api.entity;

/**
 * Direction of an FX trade from the bank's perspective.
 *
 * BUY — the bank buys the base (source) currency from the counterparty. The
 * bank applies its bid rate, which is below the mid-rate. SELL — the bank sells
 * the base (source) currency to the counterparty. The bank applies its ask
 * rate, which is above the mid-rate. MID — mid-market rate used for
 * revaluation, reporting, and internal transfers where no spread is applied.
 */
public enum TradeDirection {
    /**
     * Bank buys base currency — bid rate applies.
     */
    BUY,

    /**
     * Bank sells base currency — ask rate applies.
     */
    SELL,

    /**
     * Mid-market rate — no spread applied.
     */
    MID
}
