package com.example.macrotracker.data;

public interface TokenStorage {
    void saveTokens(String accessToken, String refreshToken);
    String getAccessToken();
    String getRefreshToken();
    void clearTokens();
}
