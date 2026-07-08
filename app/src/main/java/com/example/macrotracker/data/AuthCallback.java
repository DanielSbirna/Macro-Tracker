package com.example.macrotracker;

public interface AuthCallback {
    void onSuccess(String accessToken, String refreshToken);
    void onError(Exception e);
}
