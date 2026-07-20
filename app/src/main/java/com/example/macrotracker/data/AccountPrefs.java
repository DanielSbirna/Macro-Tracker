package com.example.macrotracker.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AccountPrefs {
    private static final String PREFS_NAME = "macro_tracker_prefs";
    private static final String KEY_ACCOUNT_COMPLETED = "account_completed";

    private final SharedPreferences prefs;

    public AccountPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAccountCompleted() {
        return prefs.getBoolean(KEY_ACCOUNT_COMPLETED, false);
    }

    public void setAccountCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ACCOUNT_COMPLETED, completed).apply();
    }
}