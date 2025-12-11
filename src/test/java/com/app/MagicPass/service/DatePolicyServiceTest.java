package com.app.MagicPass.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DatePolicyServiceTest {

    private final DatePolicyService service = new DatePolicyService();

    @Test
    void allowsTodayAndNext7Days() {
        LocalDate today = LocalDate.now();
        assertTrue(service.isWithin7Days(today));
        assertTrue(service.isWithin7Days(today.plusDays(7)));
    }

    @Test
    void rejectsPastAndBeyondWindow() {
        LocalDate today = LocalDate.now();
        assertFalse(service.isWithin7Days(today.minusDays(1)));
        assertFalse(service.isWithin7Days(today.plusDays(8)));
    }

    @Test
    void rejectsNullDate() {
        assertFalse(service.isWithin7Days(null));
    }
}
