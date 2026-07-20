package com.example.macrotracker.util;

import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.StreakStatus;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreakCalculator {

    public static class StreakResult {
        private final int streakDays;
        private final StreakStatus status;

        public StreakResult(int streakDays, StreakStatus status) {
            this.streakDays = streakDays;
            this.status = status;
        }

        public int getStreakDays() {
            return streakDays;
        }

        public StreakStatus getStatus() {
            return status;
        }

        public boolean hasStreak() {
            return streakDays > 0;
        }
    }

    /**
     * Computes the streak and status from a list of meals over a given date range.
     *
     * @param meals List of meals returned from repository.
     * @param zone  The user's local ZoneId.
     * @return StreakResult containing streak count and StreakStatus.
     */
    public static StreakResult compute(List<Meal> meals, ZoneId zone) {
        if (meals == null || meals.isEmpty()) {
            return new StreakResult(0, StreakStatus.NEEDS_ADJUSTMENT);
        }

        LocalDate today = LocalDate.now(zone);

        // Extract unique LocalDates with at least one logged meal
        Set<LocalDate> loggedDates = new HashSet<>();
        for (Meal meal : meals) {
            if (meal.getLoggedAt() != null) {
                LocalDate mealDate = meal.getLoggedAt()
                        .atZoneSameInstant(zone)
                        .toLocalDate();
                loggedDates.add(mealDate);
            }
        }

        // Calculate consecutive days backwards
        int streakDays = 0;
        LocalDate checkDate = today;

        // If today hasn't been logged yet, start checking from yesterday
        // so the active streak doesn't reset to zero mid-day.
        if (!loggedDates.contains(checkDate)) {
            checkDate = today.minusDays(1);
        }

        while (loggedDates.contains(checkDate)) {
            streakDays++;
            checkDate = checkDate.minusDays(1);
        }

        // Map streak count to your StreakStatus enum
        StreakStatus status = resolveStatus(streakDays);

        return new StreakResult(streakDays, status);
    }

    private static StreakStatus resolveStatus(int streakDays) {
        if (streakDays >= 7) {
            return StreakStatus.CRUSHING_IT;    // 7+ consecutive days
        } else if (streakDays >= 3) {
            return StreakStatus.STEADY;         // 3 to 6 days
        } else {
            return StreakStatus.NEEDS_ADJUSTMENT; // 0 to 2 days
        }
    }
}