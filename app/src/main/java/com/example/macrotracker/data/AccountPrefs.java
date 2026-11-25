package com.example.macrotracker.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AccountPrefs {
    private static final String PREFS_NAME = "macro_tracker_prefs";
    private static final String KEY_PREFIX_ACCOUNT_COMPLETED = "account_completed_";

    private final SharedPreferences prefs;

    public AccountPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAccountCompleted(String userId) {
        return prefs.getBoolean(KEY_PREFIX_ACCOUNT_COMPLETED + userId, false);
    }

    public void setAccountCompleted(String userId, boolean completed) {
        prefs.edit().putBoolean(KEY_PREFIX_ACCOUNT_COMPLETED + userId, completed).apply();
    }
}