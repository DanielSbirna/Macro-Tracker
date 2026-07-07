package com.example.macrotracker.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WeightLog {
    private Long weightId; // null until insert, DB auto increments
    private String userId; // UUID
    private BigDecimal weightValue;
    private LocalDate dateRecorded;

    // Constructor for NEW record
    public WeightLog (String userId, BigDecimal weightValue, LocalDate dateRecorded) {
        this (null, userId, weightValue, dateRecorded);
    }

    // full constructor - internal use and formJson
    public WeightLog (Long weightId, String userId, BigDecimal weightValue, LocalDate dateRecorded) {
        // checks to prevent invalid logs
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (weightValue == null
                || weightValue.compareTo(BigDecimal.ZERO) <= 0
                || weightValue.compareTo(new BigDecimal("999.99")) > 0) {
            throw new IllegalArgumentException("weightValue must be positive and below 999.99");
        }
        if (dateRecorded == null) {
            throw new IllegalArgumentException("dateRecorded cannot be null");
        }
        if (dateRecorded.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateRecorded cannot be in the future");
        }

        this.weightId = weightId;
        this.userId = userId;
        this.weightValue = weightValue;
        this.dateRecorded = dateRecorded;
    }

    // Getters
    public Long getWeightId() {return weightId;}
    public String getUserId() {return userId;}
    public BigDecimal getWeightValue() {return weightValue;}
    public LocalDate getDateRecorded() {return dateRecorded;}

    public static WeightLog fromJson(JSONObject json) throws JSONException {
        return new WeightLog(
                json.getLong("weight_id"),
                json.getString("user_id"),
                new BigDecimal(json.getString("weight_value")),
                LocalDate.parse(json.getString("date_recorded"))
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("user_id", userId);
        json.put("weight_value", weightValue.toPlainString());
        json.put("date_recorded", dateRecorded.toString());
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeightLog)) return false;
        WeightLog that = (WeightLog) o;

        if (weightId != null && that.weightId != null) {
            return weightId.equals(that.weightId);
        }
        // fallback for pre insert, compare by unique
        return userId.equals(that.userId) && dateRecorded.equals(that.dateRecorded);
    }

    @Override
    public int hashCode() {
        if (weightId != null) {
            return weightId.hashCode();
        }
        // prevention of collision - multiply by a prime number
        return userId.hashCode() * 31 + dateRecorded.hashCode();
    }

    @Override
    public String toString() {
        return "Weight logged {weightId = '" + weightId + "', userId= '" + userId +
                "', weightValue = '" + weightValue + "', dateRecorded = '" + dateRecorded + "'}";
    }
}
