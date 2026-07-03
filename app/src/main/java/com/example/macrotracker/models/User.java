package com.example.macrotracker.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.Period;
public class User {
    private String userId; // UUID from Supabase
    private String email;
    private float height;
    private LocalDate birthday;
    private char gender;
    private double activityMultiplier;
    private String goal;

    // constructor with args
    public User (String userId, String email, float height, LocalDate birthday, char gender, double activityMultiplier, String goal) {
        // basic checks to prevent invalid users
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email cannot be null and must contain '@' ");
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
        this.email = email;
        this.height = height;
        this.birthday = birthday;
        this.gender = gender;
        this.activityMultiplier = activityMultiplier;
        this.goal = goal;
    }

    // Getters
    public String getUserId() {return userId;}
    public String getEmail() {return email;}
    public float getHeight() {return height;}
    public LocalDate getBirthday() {return birthday;}
    public char getGender() {return gender;}
    public double getActivityMultiplier() {return activityMultiplier;}
    public String getGoal() {return goal;}

    // Computed age
    public int getAge() {
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    public static User fromJson(JSONObject json) throws JSONException {
        return new User(
                json.getString("user_id"),
                json.getString("email"),
                (float) json.getDouble("height"),
                LocalDate.parse(json.getString("birthday")), // expects YYYY-MM-DD format
                json.getString("gender").charAt(0),
                json.getDouble("activity_multiplier"),
                json.isNull("goal")? null : json.getString("goal")
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
        return "User {userId = '" + userId + "', email= '" + email +
                "', height = '" + height + "', birthday = '" + birthday + "', gender = '" +
                gender + "', activityMultiplier = '" + activityMultiplier + "', goal = '" + goal + "'}";
    }


}
