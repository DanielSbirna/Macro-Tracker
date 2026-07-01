package com.example.macrotracker;

public interface MacroCallback {
    void onSuccess(String responseJson);
    void onError(Exception e);
}
