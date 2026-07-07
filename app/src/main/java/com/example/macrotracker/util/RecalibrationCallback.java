package com.example.macrotracker.util;

public interface RecalibrationCallback {
    void onSuggestionAvailable(RecalibrationResult result);
    void onNoSuggestion(String reason); //not due yet or insufficient data
    void onError(Exception e);
}
