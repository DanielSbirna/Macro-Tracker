package com.example.macrotracker.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class TargetMacros {
    private Long targetId; // null until insert, DB auto increments
    private String userId;
    private OffsetDateTime createdAt;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fats;

    // Test constructor
    public TargetMacros(BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    // Constructor for NEW record
    public TargetMacros (String userId, OffsetDateTime createdAt, BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats) {
        this (null, userId, createdAt, calories, protein, carbs, fats);
    }

    // full constructor - internal use and formJson
    public TargetMacros (Long targetId, String userId, OffsetDateTime createdAt, BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fats) {
        // checks for invalid prevention
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
        if (calories == null || calories.compareTo(BigDecimal.ZERO) < 0) {
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

        this.targetId = targetId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    // Getters
    public Long getTargetId() {return targetId;}
    public String getUserId() {return userId;}
    public  OffsetDateTime getCreatedAt() {return createdAt;}
    public BigDecimal getCalories() {return calories;}
    public BigDecimal getProtein() {return protein;}
    public BigDecimal getCarbs() {return carbs;}
    public BigDecimal getFats() {return fats;}

    public static TargetMacros fromJson(JSONObject json) throws JSONException {
        return new TargetMacros(
                json.getLong("target_id"),
                json.getString("user_id"),
                OffsetDateTime.parse(json.getString("created_at")),
                new BigDecimal(json.getString("calories")),
                new BigDecimal(json.getString("protein")),
                new BigDecimal(json.getString("carbs")),
                new BigDecimal(json.getString("fats"))
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("user_id", userId);
        json.put("created_at", createdAt.toString());
        json.put("calories", calories.toPlainString());
        json.put("protein", protein.toPlainString());
        json.put("carbs", carbs.toPlainString());
        json.put("fats", fats.toPlainString());
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetMacros)) return false;
        TargetMacros that = (TargetMacros) o;

        if (targetId != null && that.targetId != null) {
            return targetId.equals(that.targetId);
        }
        // no unique constraint to fall back on pre-insert;
        // unsaved instances are only equal by reference
        return false;
    }

    @Override
    public int hashCode() {
        if (targetId != null) {
            return targetId.hashCode();
        }
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Target macros {targetId = '" + targetId + "', userId= '" + userId +
                "', createdAt = '" + createdAt + "', calories = '" + calories +
                "', protein = '" + protein +
                "', carbs = '" + carbs +
                "', fats = '" + fats +
                "'}";
    }

}
