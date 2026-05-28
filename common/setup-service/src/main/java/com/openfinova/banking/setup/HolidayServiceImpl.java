package com.openfinova.banking.setup;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.setup.api.HolidayService;
import com.openfinova.banking.setup.api.dto.HolidayDTO;
import com.openfinova.banking.setup.service.HolidayManagementService;

@Service
public class HolidayServiceImpl implements HolidayService {

    private final HolidayManagementService holidayManagementService;

    public HolidayServiceImpl(HolidayManagementService holidayManagementService) {
        this.holidayManagementService = holidayManagementService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date, String countryCode, String regionCode) {
        return holidayManagementService.isHoliday(date, countryCode, regionCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayDTO> getHolidays(int year, String countryCode, String regionCode) {
        return holidayManagementService.getHolidays(year, countryCode, regionCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayDTO> getHolidaysByCountry(int year, String countryCode) {
        return holidayManagementService.getHolidaysByCountry(year, countryCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HolidayDTO> getHoliday(LocalDate date, String countryCode, String regionCode) {
        return holidayManagementService.getHoliday(date, countryCode, regionCode);
    }

    @Override
    @Transactional
    public void addHoliday(HolidayDTO holidayDTO) {
        holidayManagementService.addHoliday(holidayDTO);
    }

    @Override
    @Transactional
    public boolean removeHoliday(LocalDate date, String countryCode, String regionCode) {
        return holidayManagementService.removeHoliday(date, countryCode, regionCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getSupportedCountries() {
        return holidayManagementService.getSupportedCountries();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getSupportedRegions(String countryCode) {
        return holidayManagementService.getSupportedRegions(countryCode);
    }

    @Override
    @Transactional
    public void initializeStandardHolidays(int year, String countryCode) {
        holidayManagementService.initializeStandardHolidays(year, countryCode);
    }

    @Override
    @Transactional
    public void clearHolidays(int year, String countryCode, String regionCode) {
        holidayManagementService.clearHolidays(year, countryCode, regionCode);
    }

    @Override
    @Transactional
    public void clearAllHolidays() {
        holidayManagementService.clearAllHolidays();
    }
}
