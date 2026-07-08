package com.example.macrotracker.util;

import com.example.macrotracker.logic.TdeeCalculator;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.models.User;
import com.example.macrotracker.models.WeightLog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecalibrationEngine {

    // 1 kg of body fat contains ~7700 kcal
    private static final BigDecimal KCAL_PER_KG = new BigDecimal("7700");
    // at least a week of logging before recalibration
    private static final int MIN_LOGGED_DAYS_REQUIRED = 7;
    // minimum days between recalibrations used by trigger
    public static final int RECALIBRATION_WINDOW_DAYS = 14;

    public static RecalibrationResult evaluate(User user, List<Meal> meals, List<WeightLog> weightLogs) {
        if (weightLogs == null || weightLogs.size() < 2) {
            return RecalibrationResult.insufficientData("Need at least two weigh-ins in this period");
        }

        Set<LocalDate> loggedDays = new HashSet<>();
        for (Meal meal : meals) {
            loggedDays.add(meal.getLoggedAt().toLocalDate());
        }
        if (loggedDays.size() < MIN_LOGGED_DAYS_REQUIRED) {
            return RecalibrationResult.insufficientData(
                    "Need at least " + MIN_LOGGED_DAYS_REQUIRED + "days of logged meals; found " + loggedDays.size() + ".");
        }
        List<WeightLog> sorted = new ArrayList<>(weightLogs);
        Collections.sort(sorted, (a, b) -> a.getDateRecorded().compareTo(b.getDateRecorded()));
        WeightLog first = sorted.get(0);
        WeightLog last = sorted.get(sorted.size() - 1);

        long daysElapsed = ChronoUnit.DAYS.between(first.getDateRecorded(), last.getDateRecorded());
        if (daysElapsed <= 0) {
            return RecalibrationResult.insufficientData("Weigh-ins must span more than one day.");
        }

        BigDecimal weightChangeKg = last.getWeightValue().subtract(first.getWeightValue());

        BigDecimal totalCaloriesEaten = BigDecimal.ZERO;
        for (Meal meal : meals) {
            LocalDate mealDate = meal.getLoggedAt().toLocalDate();
            if (!mealDate.isBefore(first.getDateRecorded()) && !mealDate.isAfter(last.getDateRecorded())) {
                totalCaloriesEaten = totalCaloriesEaten.add(meal.getCalories());
            }
        }

        BigDecimal avgDailyCalories = totalCaloriesEaten.divide(new BigDecimal(daysElapsed), 2, RoundingMode.HALF_UP);

        // real_TDEE = avg_daily_calories_eaten - (weight_change_kg * 7700 / days_elapsed)
        BigDecimal weightChangeCalories = weightChangeKg.multiply(KCAL_PER_KG)
                .divide(new BigDecimal(daysElapsed), 2, RoundingMode.HALF_UP);
        BigDecimal realTdee = avgDailyCalories.subtract(weightChangeCalories);

        BigDecimal formulaTdee = TdeeCalculator.calculateFormulaTdee(user, last.getWeightValue());
        TargetMacros suggestedTarget = TdeeCalculator.buildTargetFromTdee(user, last.getWeightValue(), realTdee);

        return RecalibrationResult.success(formulaTdee, realTdee, suggestedTarget);
    }

    // True if at least RECALIBRATION_WINDOW_DAYS have passed since the most recent target was set
    public static boolean isDueForRecalibration(TargetMacros latestTarget) {
        if (latestTarget == null) {
            return false; // no target yet at all -> nothing to recalibrate
        }
        long daysSinceLastTarget = ChronoUnit.DAYS.between(
                latestTarget.getCreatedAt().toLocalDate(), LocalDate.now());
        return daysSinceLastTarget >= RECALIBRATION_WINDOW_DAYS;
    }
}
