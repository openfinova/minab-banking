package com.openfinova.banking.setup.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class HolidayTest {

    @Test
    void constructor_derivesYearAndDefaults() {
        LocalDate date = LocalDate.of(2026, 12, 25);

        Holiday h = new Holiday(date, "US", null, "Christmas");

        assertThat(h.getDate()).isEqualTo(date);
        assertThat(h.getYear()).isEqualTo(2026);
        assertThat(h.getType()).isEqualTo(Holiday.HolidayType.PUBLIC);
        assertThat(h.getBankHoliday()).isTrue();
        assertThat(h.getObservedHoliday()).isFalse();
    }

    @Test
    void setDate_keepsYearInSync() {
        Holiday h = new Holiday(LocalDate.of(2026, 1, 1), "GB", null, "New Year");
        h.setDate(LocalDate.of(2027, 1, 1));

        assertThat(h.getYear()).isEqualTo(2027);
    }

    @Test
    void onCreate_stampsTimestampsAndAlignsYearWithDate() {
        Holiday h = new Holiday();
        h.setDate(LocalDate.of(2026, 7, 4));
        h.setYear(1900);

        h.onCreate();

        assertThat(h.getYear()).isEqualTo(2026);
        assertThat(h.getCreatedAt()).isNotNull();
        assertThat(h.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdate_refreshesUpdatedAtAndYear() {
        Holiday h = new Holiday(LocalDate.of(2026, 5, 1), "DE", "BE", "May Day");
        h.onCreate();
        LocalDateTime afterCreate = h.getUpdatedAt();

        h.setDate(LocalDate.of(2027, 5, 1));
        h.onUpdate();

        assertThat(h.getYear()).isEqualTo(2027);
        assertThat(h.getUpdatedAt()).isNotNull().isAfterOrEqualTo(afterCreate);
    }
}
