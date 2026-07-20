package com.example.macrotracker.data;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPrefs {

    private static final String PREFS_NAME = "macro_tracker_prefs";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";

    private final SharedPreferences prefs;

    public NotificationPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isNotificationEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }
}
