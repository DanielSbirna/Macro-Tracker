package com.example.macrotracker;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.example.macrotracker.data.NotificationPrefs;
import com.example.macrotracker.util.ReminderScheduler;
import com.example.macrotracker.util.ReminderWorker;

public class MacroTrackerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ReminderWorker.CHANNEL_ID,
                    getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.reminder_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        if (new NotificationPrefs(this).isNotificationEnabled()) {
            ReminderScheduler.schedule(this);
        }
    }
}