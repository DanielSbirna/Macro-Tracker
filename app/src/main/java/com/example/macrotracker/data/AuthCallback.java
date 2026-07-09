package com.example.macrotracker.data;

public interface AuthCallback {
    void onSuccess(String accessToken, String refreshToken);
    void onError(Exception e);
}
