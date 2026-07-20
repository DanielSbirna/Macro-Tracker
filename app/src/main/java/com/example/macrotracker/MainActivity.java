package com.example.macrotracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.macrotracker.data.AccountPrefs;
import com.example.macrotracker.data.NotificationPrefs;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.models.User;
import com.example.macrotracker.models.WeightLog;
import com.example.macrotracker.ui.CompleteAccountBottomDialog;
import com.example.macrotracker.ui.SettingsBottomDialog;
import com.example.macrotracker.util.JwtUtils;
import com.example.macrotracker.util.ReminderScheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                NotificationPrefs prefs = new NotificationPrefs(this);
                if (granted) {
                    if (prefs.isNotificationEnabled()) {
                        ReminderScheduler.schedule(this);
                    }
                } else {
                    prefs.setNotificationEnabled(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ServiceLocator.getInstance(this).authRepository.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new FragmentHome())
                    .commit();
        }

        setupBottomNav();
        checkAccountCompletion();
        requestNotificationPermissionIfNeeded();
    }

    // Account completion

    private void checkAccountCompletion() {
        AccountPrefs accountPrefs = new AccountPrefs(this);
        ServiceLocator serviceLocator = ServiceLocator.getInstance(this);

        if (accountPrefs.isAccountCompleted()) return;

        serviceLocator.userProfilesRepository.getProfile(new RepoCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        accountPrefs.setAccountCompleted(true);
                    } else {
                        showCompleteAccountDialog(serviceLocator, accountPrefs);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        android.util.Log.e("MainActivity", "getProfile failed, cannot verify account completion", e));
            }
        });
    }

    private void showCompleteAccountDialog(ServiceLocator serviceLocator, AccountPrefs accountPrefs) {
        CompleteAccountBottomDialog dialog = new CompleteAccountBottomDialog();
        dialog.setOnAccountCompletedListener((details, onSuccess, onFailure) -> {

            String userId;
            try {
                userId = JwtUtils.getUserIdFromToken(serviceLocator.tokenStorage.getAccessToken());
            } catch (Exception e) {
                onFailure.run();
                return;
            }

            char genderChar = "Male".equals(details.gender) ? 'M' : 'F';

            User user;
            try {
                user = new User(
                        userId,
                        details.name,
                        BigDecimal.valueOf(details.heightCm),
                        LocalDate.parse(details.birthday),
                        genderChar,
                        activityLevelToMultiplier(details.activityLevel),
                        "MAIN",
                        TimeZone.getDefault().getID(),
                        null
                );
            } catch (IllegalArgumentException e) {
                onFailure.run();
                return;
            }

            serviceLocator.userProfilesRepository.insertProfile(user, new RepoCallback<User>() {
                @Override
                public void onSuccess(User createdUser) {
                    WeightLog initialWeight = new WeightLog(
                            null,
                            userId,
                            BigDecimal.valueOf(details.weightKg),
                            LocalDate.now()
                    );

                    serviceLocator.weightLogRepository.insertWeight(initialWeight, new RepoCallback<WeightLog>() {
                        @Override
                        public void onSuccess(WeightLog loggedWeight) {
                            accountPrefs.setAccountCompleted(true);
                            runOnUiThread(onSuccess::run);
                        }

                        @Override
                        public void onError(Exception e) {
                            accountPrefs.setAccountCompleted(true);
                            runOnUiThread(onSuccess::run);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(onFailure::run);
                }
            });
        });
        dialog.show(getSupportFragmentManager(), "complete_account");
    }

    private static BigDecimal activityLevelToMultiplier(String label) {
        switch (label) {
            case "Sedentary": return new BigDecimal("1.2");
            case "Lightly active": return new BigDecimal("1.375");
            case "Moderately active": return new BigDecimal("1.55");
            case "Very active": return new BigDecimal("1.725");
            default: throw new IllegalArgumentException("Unknown activity level: " + label);
        }
    }

    // Notifications

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // Bottom nav

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            showFragment(new FragmentHome());
            setNavSelected(v);
        });
        findViewById(R.id.navFoodLog).setOnClickListener(v -> {
            showFragment(new FoodLogFragment());
            setNavSelected(v);
        });

        setNavSelected(findViewById(R.id.navHome));

        View addButton = findViewById(R.id.addButton);
        if (addButton != null) {
            addButton.setOnClickListener(v -> {
                AddDrawerFragment addDrawer = new AddDrawerFragment();
                addDrawer.show(getSupportFragmentManager(), "AddDrawerFragment");
            });
        }

        findViewById(R.id.navAssistant).setOnClickListener(v -> {
            showFragment(new AssistantFragment());
            setNavSelected(v);
        });

        findViewById(R.id.navAccount).setOnClickListener(v -> {
            ServiceLocator serviceLocator = ServiceLocator.getInstance(this);
            showFragment(AccountFragment.newInstance(
                    serviceLocator.userProfilesRepository,
                    serviceLocator.authRepository,
                    serviceLocator.weightLogRepository));
            setNavSelected(v);
        });
    }

    private void setNavSelected(View selected) {
        ImageButton[] navButtons = {
                findViewById(R.id.navHome), findViewById(R.id.navFoodLog),
                findViewById(R.id.navAssistant), findViewById(R.id.navAccount)
        };
        for (ImageButton btn : navButtons) {
            if (btn != null) {
                boolean isSelected = btn == selected;
                btn.setBackgroundResource(isSelected ? R.drawable.bg_nav_selected : android.R.color.transparent);
                if (isSelected) {
                    btn.setColorFilter(android.graphics.Color.WHITE);
                } else {
                    btn.clearColorFilter();
                }
            }
        }
    }

    private void showFragment(Fragment fragment) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current != null && current.getClass().equals(fragment.getClass())) return;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void navigateToAssistant() {
        showFragment(new AssistantFragment());
        setNavSelected(findViewById(R.id.navAssistant));
    }
}