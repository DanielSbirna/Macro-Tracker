package com.example.macrotracker.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.Period;
public class User {
    private String userId; // UUID from Supabase
    private String name;
    private float height;
    private LocalDate birthday;
    private char gender;
    private double activityMultiplier;
    private String currentGoal;
    private String timezone;

    // constructor with args
    public User (String userId, String name, float height, LocalDate birthday, char gender, double activityMultiplier, String currentGoal, String timezone) {
        // basic checks to prevent invalid users
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (name == null && name.length() > 20) {
            throw new IllegalArgumentException("name must be 20 characters or fewer");
        }
        if (height <=0 ) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (birthday == null || birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthday cannot be null or after today");
        }
        if (gender != 'M' && gender != 'F') {
            throw  new IllegalArgumentException("gender must be either 'M' or 'F' ");
        }
        if (activityMultiplier <= 0) {
            throw new IllegalArgumentException("Invalid activity level");
        }

        this.userId = userId;
        this.name = name;
        this.height = height;
        this.birthday = birthday;
        this.gender = gender;
        this.activityMultiplier = activityMultiplier;
        this.currentGoal = currentGoal;
        this.timezone = timezone;
    }

    // Getters
    public String getUserId() {return userId;}
    public String getName() {return name;}
    public float getHeight() {return height;}
    public LocalDate getBirthday() {return birthday;}
    public char getGender() {return gender;}
    public double getActivityMultiplier() {return activityMultiplier;}
    public String getCurrentGoal() {return currentGoal;}
    public String getTimezone() {return timezone;}

    // Computed age
    public int getAge() {
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    public static User fromJson(JSONObject json) throws JSONException {
        return new User(
                json.getString("user_id"),
                json.isNull("name") ? null : json.getString("name"),
                (float) json.getDouble("height"),
                LocalDate.parse(json.getString("birthday")), // expects YYYY-MM-DD format
                json.getString("gender").charAt(0),
                json.getDouble("activity_multiplier"),
                json.isNull("current_goal")? null : json.getString("current_goal"),
                json.isNull("timezone") ? null : json.getString("timezone")
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }

    @Override
    public String toString() {
        return "User {userId = '" + userId + "', name= '" + name +
                "', height = '" + height + "', birthday = '" + birthday + "', gender = '" +
                gender + "', activityMultiplier = '" + activityMultiplier + "', currentGoal = '" + currentGoal +
                "', timezone" + timezone + "'}";
    }
}
