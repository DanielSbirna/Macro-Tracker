package com.example.macrotracker.ui;

import java.time.LocalTime;
public class GreetingHelper {

    public static String getGreeting(String userName) {
        int hour = LocalTime.now().getHour();
        boolean hasName = userName != null && !userName.isEmpty();

        if (hour >= 5 && hour < 11) {
            return hasName ? "Good Morning " + userName + "! had breakfast yet?" : "Good Morning! had breakfast yet?";
        } else if (hour >= 11 && hour < 16) {
            return hasName ? "Good Afternoon " + userName + "! tracking lunch?" : "Good Afternoon! tracking lunch?";
        } else if (hour >= 16 && hour < 18) {
            return "Snacking still counts as calories";
        } else if (hour >= 18 && hour < 22) {
            return hasName ? "Good Evening " + userName + "! Dinner sounds nice" : "Good Evening! Dinner sounds nice";
        } else {
            return hasName ? "Welcome back " + userName + ", ready to track?" : "Welcome back, ready to track?";
        }
    }
}
