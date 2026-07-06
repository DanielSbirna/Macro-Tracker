package com.example.macrotracker.data;

import java.math.BigDecimal;
public class MacroEstimate {
    private final BigDecimal calories;
    private final BigDecimal protein;
    private final BigDecimal carbs;
    private final BigDecimal fats;

    public MacroEstimate(BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    public BigDecimal getCalories() {return calories;}
    public BigDecimal getProtein() {return protein;}
    public BigDecimal getCarbs() {return carbs;}
    public BigDecimal getFats() {return fats;}
}
