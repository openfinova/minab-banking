package com.openfinova.banking.setup;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.setup.api.HolidayService;
import com.openfinova.banking.setup.api.dto.HolidayDTO;
import com.openfinova.banking.setup.entity.Holiday;
import com.openfinova.banking.setup.repository.HolidayRepository;

/**
 * Implementation of HolidayService.
 *
 * This implementation stores holidays in the database using JPA.
 * It provides full CRUD operations and supports multiple countries and regions.
 *
 * Features:
 * - Persistent holiday storage in database
 * - Support for multiple countries and regions
 * - Built-in standard holidays for US, GB, CA
 * - Transaction management
 * - Thread-safe operations
 */
@Service
public class HolidayServiceImpl implements HolidayService {

    private static final Logger logger = LoggerFactory.getLogger(HolidayServiceImpl.class);

    private final HolidayRepository holidayRepository;

    public HolidayServiceImpl(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:setup:read')")
    public boolean isHoliday(LocalDate date, String countryCode, String regionCode) {
        if (regionCode == null) {
            // National-only check: exact match on null region
            return existsNationalHoliday(date, countryCode);
        }
        // Regional check: the date is a holiday if it matches either the
        // specified region OR a national (null-region) holiday for that country.
        return existsRegionalOrNational(date, countryCode, regionCode);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:setup:read')")
    public List<HolidayDTO> getHolidays(int year, String countryCode, String regionCode) {
        return listYearCountryRegion(year, countryCode, regionCode).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:setup:read')")
    public List<HolidayDTO> getHolidaysByCountry(int year, String countryCode) {
        return listYearCountry(year, countryCode).stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:setup:read')")
    public Optional<HolidayDTO> getHoliday(LocalDate date, String countryCode, String regionCode) {
        return findOne(date, countryCode, regionCode).map(this::mapToDTO);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:setup:write')")
    @CacheEvict(value = "holidays", allEntries = true)
    public void addHoliday(HolidayDTO holidayDTO) {
        Holiday entity = mapToEntity(holidayDTO);
        holidayRepository.save(entity);
        logger.debug("Added holiday: {} for {}/{}", entity.getName(), entity.getCountryCode(), entity.getRegionCode());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:setup:write')")
    @CacheEvict(value = "holidays", allEntries = true)
    public boolean removeHoliday(LocalDate date, String countryCode, String regionCode) {
        Optional<Holiday> holiday = holidayRepository
                .findByDateAndCountryCodeAndRegionCode(date, countryCode, regionCode);
        if (holiday.isPresent()) {
            holidayRepository.delete(holiday.get());
            logger.debug("Removed holiday: {} for {}/{}", holiday.get().getName(), countryCode, regionCode);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:setup:read')")
    public Set<String> getSupportedCountries() {
        return new HashSet<>(holidayRepository.findDistinctCountryCodes());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:setup:read')")
    public Set<String> getSupportedRegions(String countryCode) {
        return new HashSet<>(holidayRepository.findDistinctRegionCodesByCountryCode(countryCode));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:setup:write')")
    @CacheEvict(value = "holidays", allEntries = true)
    public void initializeStandardHolidays(int year, String countryCode) {
        switch (countryCode.toUpperCase()) {
            case "US" -> initializeUSHolidays(year);
            case "GB" -> initializeGBHolidays(year);
            case "CA" -> initializeCAHolidays(year);
            default -> logger.warn("No standard holidays defined for country: {}", countryCode);
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:setup:write')")
    @CacheEvict(value = "holidays", allEntries = true)
    public void clearHolidays(int year, String countryCode, String regionCode) {
        if (regionCode == null) {
            holidayRepository.deleteByYearAndCountryCode(year, countryCode);
        } else {
            holidayRepository.deleteByYearAndCountryCodeAndRegionCode(year, countryCode, regionCode);
        }
        logger.info("Cleared holidays for year {} in {}/{}", year, countryCode, regionCode);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:setup:write')")
    @CacheEvict(value = "holidays", allEntries = true)
    public void clearAllHolidays() {
        long count = holidayRepository.count();
        holidayRepository.deleteAll();
        logger.info("Cleared all {} holidays", count);
    }

    @Cacheable(value = "holidays", key = "#year + '_' + #countryCode + '_' + (#regionCode != null ? #regionCode : '_null_region_')")
    public List<Holiday> listYearCountryRegion(int year, String countryCode, String regionCode) {
        if (regionCode == null) {
            return holidayRepository.findByYearAndCountryCodeOrderByDateAsc(year, countryCode);
        }
        return holidayRepository.findByYearAndCountryCodeAndRegionCodeOrderByDateAsc(year, countryCode, regionCode);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "holidays", key = "'country_' + #year + '_' + #countryCode")
    public List<Holiday> listYearCountry(int year, String countryCode) {
        return holidayRepository.findByYearAndCountryCodeOrderByDateAsc(year, countryCode);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "holidays", key = "'one_' + #date + '_' + #countryCode + '_' + (#regionCode != null ? #regionCode : '_null_region_')")
    public Optional<Holiday> findOne(LocalDate date, String countryCode, String regionCode) {
        return holidayRepository.findByDateAndCountryCodeAndRegionCode(date, countryCode, regionCode);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "holidays", key = "'national_' + #date + '_' + #countryCode")
    public boolean existsNationalHoliday(LocalDate date, String countryCode) {
        return holidayRepository.existsByDateAndCountryCodeAndRegionCode(date, countryCode, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "holidays", key = "'reg_' + #date + '_' + #countryCode + '_' + #regionCode")
    public boolean existsRegionalOrNational(LocalDate date, String countryCode, String regionCode) {
        return holidayRepository.existsByDateAndCountryCodeAndRegionOrNational(date, countryCode, regionCode);
    }

    private HolidayDTO mapToDTO(Holiday entity) {
        HolidayDTO dto = new HolidayDTO();
        dto.setId(entity.getId());
        dto.setDate(entity.getDate());
        dto.setYear(entity.getYear());
        dto.setCountryCode(entity.getCountryCode());
        dto.setRegionCode(entity.getRegionCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setType(entity.getType().name());
        dto.setBankHoliday(entity.getBankHoliday());
        dto.setObservedHoliday(entity.getObservedHoliday());
        return dto;
    }

    private Holiday mapToEntity(HolidayDTO dto) {
        Holiday entity = new Holiday();
        entity.setId(dto.getId());
        entity.setDate(dto.getDate());
        entity.setCountryCode(dto.getCountryCode());
        entity.setRegionCode(dto.getRegionCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        if (dto.getType() != null) {
            entity.setType(Holiday.HolidayType.valueOf(dto.getType()));
        }
        entity.setBankHoliday(dto.getBankHoliday());
        entity.setObservedHoliday(dto.getObservedHoliday());
        return entity;
    }

    // ==================== US Holidays ====================

    private void initializeUSHolidays(int year) {
        logger.info("Initializing US holidays for year {}", year);

        addUSHoliday(year, LocalDate.of(year, Month.JANUARY, 1), "New Year's Day", "First day of the year");

        LocalDate mlkDay = LocalDate.of(year, Month.JANUARY, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY));
        addUSHoliday(year, mlkDay, "Martin Luther King Jr. Day", "Birthday of Martin Luther King Jr.");

        LocalDate presidentsDay = LocalDate.of(year, Month.FEBRUARY, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY));
        addUSHoliday(year, presidentsDay, "Presidents' Day", "Washington's Birthday");

        LocalDate memorialDay = LocalDate.of(year, Month.MAY, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY));
        addUSHoliday(year, memorialDay, "Memorial Day", "Honoring military personnel who died in service");

        addUSHoliday(year, LocalDate.of(year, Month.JULY, 4), "Independence Day", "Declaration of Independence");

        LocalDate laborDay = LocalDate.of(year, Month.SEPTEMBER, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        addUSHoliday(year, laborDay, "Labor Day", "Celebration of the American labor movement");

        LocalDate columbusDay = LocalDate.of(year, Month.OCTOBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY));
        addUSHoliday(year, columbusDay, "Columbus Day", "Arrival of Christopher Columbus in the Americas");

        addUSHoliday(year, LocalDate.of(year, Month.NOVEMBER, 11), "Veterans Day", "Honoring military veterans");

        LocalDate thanksgiving = LocalDate.of(year, Month.NOVEMBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY));
        addUSHoliday(year, thanksgiving, "Thanksgiving Day", "Day of giving thanks");

        addUSHoliday(
                year,
                LocalDate.of(year, Month.DECEMBER, 25),
                "Christmas Day",
                "Celebration of the birth of Jesus Christ");

        logger.info("Initialized 10 US holidays for year {}", year);
    }

    private void addUSHoliday(int year, LocalDate date, String name, String description) {
        Holiday holiday = new Holiday(date, "US", null, name);
        holiday.setDescription(description);
        holiday.setType(Holiday.HolidayType.PUBLIC);
        holiday.setBankHoliday(true);
        holidayRepository.save(holiday);
    }

    // ==================== UK/GB Holidays ====================

    private void initializeGBHolidays(int year) {
        logger.info("Initializing GB holidays for year {}", year);

        addGBHoliday(year, LocalDate.of(year, Month.JANUARY, 1), "New Year's Day", "First day of the year");

        LocalDate goodFriday = calculateEaster(year).minusDays(2);
        addGBHoliday(year, goodFriday, "Good Friday", "Friday before Easter");

        LocalDate easterMonday = calculateEaster(year).plusDays(1);
        addGBHoliday(year, easterMonday, "Easter Monday", "Monday after Easter");

        LocalDate earlyMay = LocalDate.of(year, Month.MAY, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        addGBHoliday(year, earlyMay, "Early May Bank Holiday", "Spring bank holiday");

        LocalDate springBank = LocalDate.of(year, Month.MAY, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY));
        addGBHoliday(year, springBank, "Spring Bank Holiday", "Late spring bank holiday");

        LocalDate summerBank = LocalDate.of(year, Month.AUGUST, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY));
        addGBHoliday(year, summerBank, "Summer Bank Holiday", "Late summer bank holiday");

        addGBHoliday(
                year,
                LocalDate.of(year, Month.DECEMBER, 25),
                "Christmas Day",
                "Celebration of the birth of Jesus Christ");

        addGBHoliday(year, LocalDate.of(year, Month.DECEMBER, 26), "Boxing Day", "Day after Christmas");

        logger.info("Initialized 8 GB holidays for year {}", year);
    }

    private void addGBHoliday(int year, LocalDate date, String name, String description) {
        Holiday holiday = new Holiday(date, "GB", null, name);
        holiday.setDescription(description);
        holiday.setType(Holiday.HolidayType.BANK);
        holiday.setBankHoliday(true);
        holidayRepository.save(holiday);
    }

    // ==================== Canada Holidays ====================

    private void initializeCAHolidays(int year) {
        logger.info("Initializing CA holidays for year {}", year);

        addCAHoliday(year, LocalDate.of(year, Month.JANUARY, 1), "New Year's Day", "First day of the year");

        LocalDate goodFriday = calculateEaster(year).minusDays(2);
        addCAHoliday(year, goodFriday, "Good Friday", "Friday before Easter");

        LocalDate victoriaDay = LocalDate.of(year, Month.MAY, 25).with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        addCAHoliday(year, victoriaDay, "Victoria Day", "Queen Victoria's birthday");

        addCAHoliday(year, LocalDate.of(year, Month.JULY, 1), "Canada Day", "Anniversary of Canadian Confederation");

        LocalDate labourDay = LocalDate.of(year, Month.SEPTEMBER, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        addCAHoliday(year, labourDay, "Labour Day", "Celebration of workers");

        LocalDate thanksgiving = LocalDate.of(year, Month.OCTOBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY));
        addCAHoliday(year, thanksgiving, "Thanksgiving", "Day of giving thanks");

        addCAHoliday(
                year,
                LocalDate.of(year, Month.DECEMBER, 25),
                "Christmas Day",
                "Celebration of the birth of Jesus Christ");

        addCAHoliday(year, LocalDate.of(year, Month.DECEMBER, 26), "Boxing Day", "Day after Christmas");

        logger.info("Initialized 8 CA holidays for year {}", year);
    }

    private void addCAHoliday(int year, LocalDate date, String name, String description) {
        Holiday holiday = new Holiday(date, "CA", null, name);
        holiday.setDescription(description);
        holiday.setType(Holiday.HolidayType.PUBLIC);
        holiday.setBankHoliday(true);
        holidayRepository.save(holiday);
    }

    // ==================== Easter Calculation ====================

    private LocalDate calculateEaster(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }
}
