package com.app.MagicPass.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import org.springframework.stereotype.Service;

@Service
public class DatePolicyService {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public LocalDate parseAndValidateReservationDate(String input) {
        LocalDate date;
        try {
            date = LocalDate.parse(input, FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong format. Please re-enter (dd/MM/yyyy).");
        }

        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(7))) {
            throw new IllegalArgumentException("We only provide reservation within 7 days. Please re-enter.");
        }
        return date;
    }
}
