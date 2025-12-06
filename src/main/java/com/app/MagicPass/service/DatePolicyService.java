package com.app.MagicPass.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DatePolicyService {

    public boolean isWithin7Days(LocalDate date) {
        if (date == null) return false;

        LocalDate today = LocalDate.now();
        LocalDate latest = today.plusDays(7);

        return !date.isBefore(today) && !date.isAfter(latest);
    }
}
