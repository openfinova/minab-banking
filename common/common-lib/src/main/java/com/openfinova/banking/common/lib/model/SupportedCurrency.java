package com.openfinova.banking.common.lib.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enum representing ISO 4217 currency codes.
 * Includes all major international currencies supported by the system.
 *
 * Each currency includes its decimal places (minor units) as defined by ISO 4217.
 * This is critical for:
 * - Proper rounding in calculations
 * - FX conversion tolerance validation
 * - Display formatting
 */
public enum SupportedCurrency {
    // Major currencies
    USD("United States Dollar", 2),
    EUR("Euro", 2),
    GBP("British Pound Sterling", 2),
    JPY("Japanese Yen", 0),
    CHF("Swiss Franc", 2),
    CAD("Canadian Dollar", 2),
    AUD("Australian Dollar", 2),
    NZD("New Zealand Dollar", 2),

    // Asian currencies
    CNY("Chinese Yuan", 2),
    HKD("Hong Kong Dollar", 2),
    SGD("Singapore Dollar", 2),
    INR("Indian Rupee", 2),
    KRW("South Korean Won", 0),
    THB("Thai Baht", 2),
    MYR("Malaysian Ringgit", 2),
    IDR("Indonesian Rupiah", 0),
    PHP("Philippine Peso", 2),
    VND("Vietnamese Dong", 0),
    PKR("Pakistani Rupee", 2),
    BDT("Bangladeshi Taka", 2),
    LKR("Sri Lankan Rupee", 2),
    NPR("Nepalese Rupee", 2),

    // Middle Eastern currencies
    SAR("Saudi Riyal", 2),
    AED("UAE Dirham", 2),
    QAR("Qatari Riyal", 2),
    KWD("Kuwaiti Dinar", 3),
    BHD("Bahraini Dinar", 3),
    OMR("Omani Rial", 3),
    JOD("Jordanian Dinar", 3),
    ILS("Israeli New Shekel", 2),
    TRY("Turkish Lira", 2),
    IQD("Iraqi Dinar", 3),
    IRR("Iranian Rial", 2),

    // European currencies
    SEK("Swedish Krona", 2),
    NOK("Norwegian Krone", 2),
    DKK("Danish Krone", 2),
    PLN("Polish Zloty", 2),
    CZK("Czech Koruna", 2),
    HUF("Hungarian Forint", 0),
    RON("Romanian Leu", 2),
    BGN("Bulgarian Lev", 2),
    RSD("Serbian Dinar", 2),
    UAH("Ukrainian Hryvnia", 2),
    RUB("Russian Ruble", 2),
    ISK("Icelandic Krona", 0),

    // African currencies
    ZAR("South African Rand", 2),
    NGN("Nigerian Naira", 2),
    EGP("Egyptian Pound", 2),
    KES("Kenyan Shilling", 2),
    GHS("Ghanaian Cedi", 2),
    TZS("Tanzanian Shilling", 2),
    UGX("Ugandan Shilling", 0),
    MAD("Moroccan Dirham", 2),
    TND("Tunisian Dinar", 3),
    DZD("Algerian Dinar", 2),
    AOA("Angolan Kwanza", 2),
    ETB("Ethiopian Birr", 2),
    MUR("Mauritian Rupee", 2),

    // Latin American currencies
    MXN("Mexican Peso", 2),
    BRL("Brazilian Real", 2),
    ARS("Argentine Peso", 2),
    CLP("Chilean Peso", 0),
    COP("Colombian Peso", 2),
    PEN("Peruvian Sol", 2),
    VES("Venezuelan Bolívar", 2),
    UYU("Uruguayan Peso", 2),
    BOB("Bolivian Boliviano", 2),
    PYG("Paraguayan Guarani", 0),
    CRC("Costa Rican Colón", 2),
    GTQ("Guatemalan Quetzal", 2),
    DOP("Dominican Peso", 2),
    JMD("Jamaican Dollar", 2),
    TTD("Trinidad and Tobago Dollar", 2),

    // Other currencies
    FJD("Fijian Dollar", 2),
    PGK("Papua New Guinean Kina", 2),
    WST("Samoan Tala", 2),
    TOP("Tongan Paʻanga", 2),
    SBD("Solomon Islands Dollar", 2),
    VUV("Vanuatu Vatu", 0);

    private final String description;
    private final int decimalPlaces;

    SupportedCurrency(String description, int decimalPlaces) {
        this.description = description;
        this.decimalPlaces = decimalPlaces;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the number of decimal places (minor units) for this currency as per ISO 4217.
     *
     * Examples:
     * - USD, EUR, GBP: 2 (cents)
     * - JPY, KRW: 0 (no minor unit)
     * - KWD, BHD, OMR: 3 (fils)
     *
     * This is used for:
     * - Rounding amounts to the correct precision
     * - Calculating FX conversion tolerance
     * - Display formatting
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /**
     * Get the FX conversion tolerance for this currency.
     * Tolerance is 1 unit at the smallest decimal place.
     *
     * Examples:
     * - USD (2 decimals): 0.01 (1 cent)
     * - JPY (0 decimals): 1.00 (1 yen)
     * - KWD (3 decimals): 0.001 (1 fils)
     *
     * Use this for validating FX calculations with rounding tolerance.
     */
    public java.math.BigDecimal getFxTolerance() {
        return java.math.BigDecimal.ONE.scaleByPowerOfTen(-decimalPlaces);
    }

    public String getCode() {
        return this.name();
    }

    /**
     * Returns a set of all currency codes.
     */
    public static Set<String> getAllCodes() {
        return Arrays.stream(values()).map(SupportedCurrency::name).collect(Collectors.toSet());
    }

    /**
     * Checks if a currency code is valid.
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        try {
            SupportedCurrency.valueOf(code.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets a SupportedCurrency enum from a code string.
     */
    public static SupportedCurrency fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Currency code cannot be null");
        }
        return SupportedCurrency.valueOf(code.toUpperCase());
    }
}
