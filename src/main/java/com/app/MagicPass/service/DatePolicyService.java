package com.app.MagicPass.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DatePolicyService {
    private static final int MAX_DAYS_AHEAD = 7;

    public boolean isWithin7Days(LocalDate date) {
        if (date == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate min = today.plusDays(1); // must be at least tomorrow
        LocalDate max = today.plusDays(MAX_DAYS_AHEAD);
        return !date.isBefore(min) && !date.isAfter(max);
    }
}
