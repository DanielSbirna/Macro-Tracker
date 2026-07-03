package com.example.macrotracker;

public interface AuthCallback {
    void onSuccess(String accessToken);
    void onError(Exception e);
}
