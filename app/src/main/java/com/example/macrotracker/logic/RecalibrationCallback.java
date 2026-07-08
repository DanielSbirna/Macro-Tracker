package com.example.macrotracker.util;

import com.example.macrotracker.logic.RecalibrationResult;

public interface RecalibrationCallback {
    void onSuggestionAvailable(RecalibrationResult result);
    void onNoSuggestion(String reason); //not due yet or insufficient data
    void onError(Exception e);
}
