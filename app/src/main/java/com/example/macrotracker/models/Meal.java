package com.example.macrotracker.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Meal {

    public enum MealType {
        breakfast,
        lunch,
        snack,
        dinner
    }

    private Long mealId; // null until db insert
    private String userId;
    private String title;
    private MealType type;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fats;
    private OffsetDateTime loggedAt;

    // Constructor for NEW record
    public Meal (String userId, String title, MealType type, BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats, OffsetDateTime loggedAt) {
        this(null, userId, title, type, calories, protein, carbs, fats, loggedAt);
    }

    // full constructor
    public Meal (Long mealId, String userId, String title, MealType type, BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats, OffsetDateTime loggedAt) {
        // validations to prevent invalid meal
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (title == null || title.isEmpty() || title.length() > 100) {
            throw new IllegalArgumentException("title cannot be null or empty or exceed 100 characters");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (calories == null || calories.compareTo(BigDecimal.ZERO) < 0 ) {
            throw new IllegalArgumentException("calories cannot be null or negative");
        }
        if (protein == null || protein.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("protein cannot be null or negative");
        }
        if (carbs == null || carbs.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("carbs cannot be null or negative");
        }
        if (fats == null || fats.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("fats cannot be null or negative");
        }
        if (loggedAt == null) {
            throw new IllegalArgumentException("loggedAt cannot be null");
        }

        this.mealId = mealId;
        this.userId = userId;
        this.title = title;
        this.type = type;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.loggedAt = loggedAt;
    }

    // Getters
    public Long getMealId() {return mealId;}
    public String getUserId() {return userId;}
    public String getTitle() {return title;}
    public MealType getType() {return type;}
    public BigDecimal getCalories() {return calories;}
    public BigDecimal getProtein() {return protein;}
    public BigDecimal getCarbs()  {return carbs;}
    public BigDecimal getFats() {return fats;}
    public OffsetDateTime getLoggedAt() {return loggedAt;}

    public Meal fromJson(JSONObject json) throws JSONException {
        return new Meal(
                json.getLong("meal_id"),
                json.getString("user_id"),
                json.getString("title"),
                MealType.valueOf(json.getString("type")),
                new BigDecimal(json.getString("calories")),
                new BigDecimal(json.getString("protein")),
                new BigDecimal(json.getString("carbs")),
                new BigDecimal(json.getString("fats")),
                OffsetDateTime.parse(json.getString("logged_at"))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meal)) return false;
        Meal that = (Meal) o;

        if (mealId != null && that.mealId != null) {
            return mealId.equals(that.mealId);
        }
        // no unique constraint to fall back on pre insert
        // unsaved instances are only equal by reference

        return false;
    }

    @Override
    public int hashCode() {
        if (mealId != null) {
            return mealId.hashCode();
        }
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Meal {mealId = '" + mealId + "', userId = '" + userId + "', title = '" + title +
                "', type = '" + type + "', calories = '" + calories + "', protein = '" + protein +
                "', carbs = '" + carbs + "', fats = '" + fats + "', loggedAt = '" + loggedAt + "'}";
    }

}
