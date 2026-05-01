package com.openfinova.banking.customer.account.api.dto;

import java.time.LocalDate;

/**
 * DTO representing an available statement period.
 */
public class StatementPeriod {

    private int year;
    private int month;
    private LocalDate fromDate;
    private LocalDate toDate;

    public StatementPeriod() {
    }

    public StatementPeriod(int year, int month) {
        this.year = year;
        this.month = month;
        this.fromDate = LocalDate.of(year, month, 1);
        this.toDate = fromDate.plusMonths(1).minusDays(1);
    }

    public StatementPeriod(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
