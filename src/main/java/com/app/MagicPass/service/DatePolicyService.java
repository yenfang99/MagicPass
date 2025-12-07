package com.app.MagicPass.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DatePolicyService {
    private static final int MAX_DAYS_AHEAD = 7;

    public boolean isWithin7Days(LocalDate date) {
        if (date == null) return false;
        LocalDate today = LocalDate.now();
        return !date.isBefore(today) && !date.isAfter(today.plusDays(MAX_DAYS_AHEAD));
    }
}
