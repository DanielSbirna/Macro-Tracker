package com.example.macrotracker.models;

import java.math.BigDecimal;
public class MacroEstimate {
    private final BigDecimal calories;
    private final BigDecimal protein;
    private final BigDecimal carbs;
    private final BigDecimal fats;
    private final String suggestion;

    public MacroEstimate(BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats, String suggestion) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.suggestion = suggestion;
    }

    public BigDecimal getCalories() {return calories;}
    public BigDecimal getProtein() {return protein;}
    public BigDecimal getCarbs() {return carbs;}
    public BigDecimal getFats() {return fats;}
    public String getSuggestion() {return suggestion;}
}
