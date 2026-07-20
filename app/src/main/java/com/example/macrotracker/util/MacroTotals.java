package com.example.macrotracker.util;

import com.example.macrotracker.models.Meal;

import java.math.BigDecimal;
import java.util.List;
public class MacroTotals {
    public final BigDecimal calories;
    public final BigDecimal protein;
    public final BigDecimal carbs;
    public final BigDecimal fats;

    public MacroTotals(BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    public static final MacroTotals ZERO = new MacroTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public static MacroTotals sum(List<Meal> meals) {
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fats = BigDecimal.ZERO;
        for (Meal meal : meals) {
            calories = calories.add(meal.getCalories());
            protein = protein.add(meal.getProtein());
            carbs = carbs.add(meal.getCarbs());
            fats = fats.add(meal.getFats());
        }
        return new MacroTotals(calories, protein, carbs, fats);
    }

    // Assistant Fragment
    public MacroTotals plus(BigDecimal addCalories, BigDecimal addProtein, BigDecimal addCarbs, BigDecimal addFats) {
        return new MacroTotals(
                calories.add(addCalories),
                protein.add(addProtein),
                carbs.add(addCarbs),
                fats.add(addFats)
        );
    }
}
