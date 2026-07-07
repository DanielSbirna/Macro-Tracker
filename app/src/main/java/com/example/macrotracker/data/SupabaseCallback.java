package com.example.macrotracker.data;

public interface SupabaseCallback {
    void onSuccess(String responseBody);
    void onError(Exception e);
}