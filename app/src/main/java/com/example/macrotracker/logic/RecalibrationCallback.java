package com.example.macrotracker.logic;

public interface RecalibrationCallback {
    void onSuggestionAvailable(RecalibrationResult result);
    void onNoSuggestion(String reason); //not due yet or insufficient data
    void onError(Exception e);
}
