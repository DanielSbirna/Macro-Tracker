package com.example.macrotracker.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateUtils {
    // Returns [dayStart, dayEnd] in the given timezone for the given calendar date.
    public static OffsetDateTime[] dayBounds(LocalDate date, String timezoneId) {
        ZoneId zone = ZoneId.of(timezoneId);
        ZonedDateTime start = date.atStartOfDay(zone);
        ZonedDateTime end = start.plusDays(1);
        return new OffsetDateTime[] { start.toOffsetDateTime(), end.toOffsetDateTime() };
    }
}
