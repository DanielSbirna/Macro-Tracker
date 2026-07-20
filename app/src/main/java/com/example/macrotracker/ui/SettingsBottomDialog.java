package com.example.macrotracker.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.macrotracker.R;
import com.example.macrotracker.data.NotificationPrefs;
import com.example.macrotracker.util.ReminderScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsBottomDialog extends BottomSheetDialogFragment {

    // TODO: swap in your real hosted URLs
    private static final String PRIVACY_POLICY_URL = "";
    private static final String TERMS_OF_SERVICE_URL = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_bottom_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NotificationPrefs prefs = new NotificationPrefs(requireContext());
        SwitchMaterial notificationSwitch = view.findViewById(R.id.notificationSwitch);
        notificationSwitch.setChecked(prefs.isNotificationEnabled());
        notificationSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setNotificationEnabled(isChecked);
            if (isChecked) {
                ReminderScheduler.schedule(requireContext());
            } else {
                ReminderScheduler.cancel(requireContext());
            }
        });

        view.findViewById(R.id.privacyPolicyLink).setOnClickListener(v -> openUrl(PRIVACY_POLICY_URL));
        view.findViewById(R.id.thermsOfServiceLink).setOnClickListener(v -> openUrl(TERMS_OF_SERVICE_URL));
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}