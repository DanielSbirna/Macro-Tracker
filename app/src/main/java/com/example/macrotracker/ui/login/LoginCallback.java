package com.example.macrotracker.ui.login;

import com.example.macrotracker.models.User;

public interface LoginCallback {
    void onLoggedIn(User user); // null means: route to onboarding
    void onBlocked(String reason);
    void onError(Exception e);
}