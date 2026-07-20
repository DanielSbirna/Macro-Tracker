package com.example.macrotracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.macrotracker.data.AuthCallback;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.repository.AuthRepository;
import com.example.macrotracker.data.repository.UserProfilesRepository;
import com.example.macrotracker.data.repository.WeightLogRepository;
import com.example.macrotracker.models.User;
import com.example.macrotracker.models.WeightLog;
import com.example.macrotracker.ui.SettingsBottomDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AccountFragment extends Fragment {

    private UserProfilesRepository userProfilesRepository;
    private AuthRepository authRepository;
    private WeightLogRepository weightLogRepository;

    private User currentUser;
    private String currentEmail;
    private WeightLog latestWeightLog;
    private WeightLog earliestWeightLog;

    private ImageView accountAvatar;
    private TextView userName;
    private TextView userEmail;
    private TextView userHeight;
    private TextView userWeight;
    private TextView userBirthday;
    private TextView userActivity;
    private TextView userGoal;
    private TextView userStartDay;
    private TextView userStartWeight;

    private static final String[] ACTIVITY_LABELS = {
            "Sedentary", "Lightly active", "Moderately active", "Very active", "Extremely active"
    };
    private static final BigDecimal[] ACTIVITY_VALUES = {
            new BigDecimal("1.2"), new BigDecimal("1.375"), new BigDecimal("1.55"),
            new BigDecimal("1.725"), new BigDecimal("1.9")
    };

    private String[] goalLabels;
    private static final String[] GOAL_VALUES = {"cut", "maintain", "bulk"};

    private static final DateTimeFormatter BIRTHDAY_DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());

    public AccountFragment() {
        // Required empty public constructor
    }

    public static AccountFragment newInstance(UserProfilesRepository userProfilesRepository,
                                              AuthRepository authRepository,
                                              WeightLogRepository weightLogRepository) {
        AccountFragment fragment = new AccountFragment();
        fragment.userProfilesRepository = userProfilesRepository;
        fragment.authRepository = authRepository;
        fragment.weightLogRepository = weightLogRepository;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (userProfilesRepository == null || authRepository == null || weightLogRepository == null) {
            Toast.makeText(requireContext(),
                    "AccountFragment missing repositories — use AccountFragment.newInstance(...)",
                    Toast.LENGTH_LONG).show();
            return;
        }

        bindViews(view);
        loadProfile();

        view.findViewById(R.id.settingsBtn).setOnClickListener(v ->
                new SettingsBottomDialog().show(getParentFragmentManager(), "settings"));
    }

    private void bindViews(View view) {
        goalLabels = new String[] {
                getString(R.string.goal_cut),
                getString(R.string.goal_main),
                getString(R.string.goal_bulk)
        };

        accountAvatar = view.findViewById(R.id.accountAvatar);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        userHeight = view.findViewById(R.id.userHeight);
        userWeight = view.findViewById(R.id.userWeight);
        userBirthday = view.findViewById(R.id.userBirthday);
        userActivity = view.findViewById(R.id.userActivity);
        userGoal = view.findViewById(R.id.userGoal);
        userStartDay = view.findViewById(R.id.userStartDay);
        userStartWeight = view.findViewById(R.id.userStartWeight);

        ImageButton editHeightBtn = view.findViewById(R.id.editHeightBtn);
        ImageButton editWeightBtn = view.findViewById(R.id.editWeightBtn);
        ImageButton editBirthdayBtn = view.findViewById(R.id.editBirthdayBtn);
        ImageButton editActivityBtn = view.findViewById(R.id.editActivityBtn);
        ImageButton editGoalBtn = view.findViewById(R.id.editGoalBtn);

        editHeightBtn.setOnClickListener(v -> showEditHeightSheet());
        editWeightBtn.setOnClickListener(v -> showEditWeightSheet());
        editBirthdayBtn.setOnClickListener(v -> showEditBirthdaySheet());
        editActivityBtn.setOnClickListener(v -> showEditActivitySheet());
        editGoalBtn.setOnClickListener(v -> showEditGoalSheet());

        view.findViewById(R.id.logOutBtn).setOnClickListener(v -> confirmLogOut());
        view.findViewById(R.id.deleteBtn).setOnClickListener(v -> confirmDeleteAccount());
    }

    private void loadProfile() {
        currentEmail = safeGetEmailFromToken();

        userProfilesRepository.getProfile(new RepoCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUi(() -> {
                    if (user == null) {
                        Toast.makeText(requireContext(),
                                "No profile found — complete onboarding first.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    currentUser = user;
                    renderUser(user);
                    loadWeightHistory();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUi(() -> Toast.makeText(requireContext(),
                        "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadWeightHistory() {
        weightLogRepository.getWeightHistory(new RepoCallback<List<WeightLog>>() {
            @Override
            public void onSuccess(List<WeightLog> logs) {
                runOnUi(() -> {
                    if (logs == null || logs.isEmpty()) {
                        latestWeightLog = null;
                        earliestWeightLog = null;
                    } else {
                        latestWeightLog = logs.get(0);
                        earliestWeightLog = logs.get(logs.size() - 1);
                    }
                    renderWeightHistory();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUi(() -> Toast.makeText(requireContext(),
                        "Failed to load weight history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderWeightHistory() {
        userWeight.setText(formatWeight(latestWeightLog));
        userStartWeight.setText(formatWeight(earliestWeightLog));
        userStartDay.setText(earliestWeightLog == null
                ? "-"
                : earliestWeightLog.getDateRecorded().format(BIRTHDAY_DISPLAY_FORMAT));
    }

    private String formatWeight(WeightLog log) {
        return log == null ? "-" : log.getWeightValue().setScale(1, RoundingMode.HALF_UP) + " kg";
    }

    private String safeGetEmailFromToken() {
        String token = authRepository.getAccessToken();
        if (token == null) return null;

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            byte[] decoded = Base64.decode(parts[1],
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject payload = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            return payload.optString("email", null);
        } catch (Exception e) {
            return null;
        }
    }

    private void renderUser(User user) {
        userName.setText(user.getName() != null ? user.getName() : "-");
        userEmail.setText(currentEmail != null ? currentEmail : "-");
        userHeight.setText(formatHeight(user.getHeight()));
        userBirthday.setText(formatBirthday(user.getBirthday()));
        userActivity.setText(activityLabelFor(user.getActivityMultiplier()));
        userGoal.setText(goalLabelFor(user.getCurrentGoal()));

        accountAvatar.setImageResource(
                user.getGender() == 'F' ? R.drawable.avatar_female : R.drawable.avatar_male);
    }

    private String formatHeight(BigDecimal height) {
        return height == null ? "-" : height.setScale(1, RoundingMode.HALF_UP) + " cm";
    }

    private String formatBirthday(LocalDate birthday) {
        if (birthday == null) return "-";
        String formatted = birthday.format(BIRTHDAY_DISPLAY_FORMAT);
        int age = currentUser != null ? currentUser.getAge() : 0;
        return formatted + " (" + age + ")";
    }

    private String activityLabelFor(BigDecimal multiplier) {
        if (multiplier == null) return "-";
        for (int i = 0; i < ACTIVITY_VALUES.length; i++) {
            if (ACTIVITY_VALUES[i].compareTo(multiplier) == 0) {
                return ACTIVITY_LABELS[i];
            }
        }
        return multiplier.toPlainString(); // fallback for a value outside the presets
    }

    private String goalLabelFor(String goal) {
        if (goal == null) return "-";
        for (int i = 0; i < GOAL_VALUES.length; i++) {
            if (GOAL_VALUES[i].equalsIgnoreCase(goal)) {
                return goalLabels[i];
            }
        }
        return goal; // fallback for a value outside the presets
    }

    // Bottom sheets
    private View inflateSheet(int layoutRes) {
        return LayoutInflater.from(requireContext()).inflate(layoutRes, null, false);
    }

    private void showEditHeightSheet() {
        if (currentUser == null) return;

        View sheetView = inflateSheet(R.layout.bottom_dialog_edit_height);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        EditText input = sheetView.findViewById(R.id.heightInput);
        input.setText(currentUser.getHeight().toPlainString());

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.saveBtn).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                input.setError("Enter a height");
                return;
            }
            try {
                BigDecimal newHeight = new BigDecimal(raw);
                saveProfileUpdate(withHeight(currentUser, newHeight));
                dialog.dismiss();
            } catch (NumberFormatException e) {
                input.setError("Enter a valid height");
            }
        });

        dialog.show();
    }

    private void showEditWeightSheet() {
        if (currentUser == null) return;

        View sheetView = inflateSheet(R.layout.bottom_dialog_edit_weight);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        EditText input = sheetView.findViewById(R.id.weightInput);
        if (latestWeightLog != null) {
            input.setText(latestWeightLog.getWeightValue().toPlainString());
        }

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.saveBtn).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                input.setError("Enter a weight");
                return;
            }

            WeightLog newLog;
            try {
                BigDecimal newWeight = new BigDecimal(raw);
                newLog = new WeightLog(currentUser.getUserId(), newWeight, LocalDate.now());
            } catch (NumberFormatException e) {
                input.setError("Enter a valid weight");
                return;
            } catch (IllegalArgumentException e) {
                input.setError(e.getMessage());
                return;
            }

            weightLogRepository.insertWeight(newLog, new RepoCallback<WeightLog>() {
                @Override
                public void onSuccess(WeightLog result) {
                    runOnUi(() -> {
                        Toast.makeText(requireContext(), "Weight logged", Toast.LENGTH_SHORT).show();
                        loadWeightHistory();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUi(() -> Toast.makeText(requireContext(),
                            "Failed to log weight: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditBirthdaySheet() {
        if (currentUser == null) return;

        View sheetView = inflateSheet(R.layout.bottom_dialog_edit_birthday);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        TextView birthdayValue = sheetView.findViewById(R.id.birthdayValue);
        View birthdayField = sheetView.findViewById(R.id.birthdayField);

        final LocalDate[] selectedBirthday = {currentUser.getBirthday()};
        birthdayValue.setText(selectedBirthday[0].format(BIRTHDAY_DISPLAY_FORMAT));

        birthdayField.setOnClickListener(v -> {
            long initialSelectionMillis = selectedBirthday[0]
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();

            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setEnd(System.currentTimeMillis())
                    .build();

            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.update_birthday))
                    .setSelection(initialSelectionMillis)
                    .setCalendarConstraints(constraints)
                    .build();

            picker.addOnPositiveButtonClickListener(selectionMillis -> {
                LocalDate picked = Instant.ofEpochMilli(selectionMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();
                selectedBirthday[0] = picked;
                birthdayValue.setText(picked.format(BIRTHDAY_DISPLAY_FORMAT));
            });

            picker.show(getParentFragmentManager(), "birthday_picker");
        });

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.saveBtn).setOnClickListener(v -> {
            try {
                saveProfileUpdate(withBirthday(currentUser, selectedBirthday[0]));
                dialog.dismiss();
            } catch (IllegalArgumentException e) {
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEditActivitySheet() {
        if (currentUser == null) return;

        View sheetView = inflateSheet(R.layout.bottom_dialog_edit_activity);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        AutoCompleteTextView dropdown = sheetView.findViewById(R.id.activityDropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown, ACTIVITY_LABELS);
        dropdown.setAdapter(adapter);

        final BigDecimal[] selectedMultiplier = {currentUser.getActivityMultiplier()};
        int currentIndex = -1;
        for (int i = 0; i < ACTIVITY_VALUES.length; i++) {
            if (ACTIVITY_VALUES[i].compareTo(currentUser.getActivityMultiplier()) == 0) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex >= 0) {
            dropdown.setText(ACTIVITY_LABELS[currentIndex], false);
        }

        dropdown.setOnItemClickListener((parent, view, position, id) ->
                selectedMultiplier[0] = ACTIVITY_VALUES[position]);
        dropdown.setOnClickListener(v -> dropdown.showDropDown());

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.saveBtn).setOnClickListener(v -> {
            saveProfileUpdate(withActivity(currentUser, selectedMultiplier[0]));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditGoalSheet() {
        if (currentUser == null) return;

        View sheetView = inflateSheet(R.layout.bottom_dialog_edit_goal);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        AutoCompleteTextView dropdown = sheetView.findViewById(R.id.goalDropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown, goalLabels);
        dropdown.setAdapter(adapter);

        final String[] selectedGoal = {currentUser.getCurrentGoal()};
        int currentIndex = -1;
        for (int i = 0; i < GOAL_VALUES.length; i++) {
            if (GOAL_VALUES[i].equalsIgnoreCase(currentUser.getCurrentGoal())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex >= 0) {
            dropdown.setText(goalLabels[currentIndex], false);
        }

        dropdown.setOnItemClickListener((parent, view, position, id) ->
                selectedGoal[0] = GOAL_VALUES[position]);
        dropdown.setOnClickListener(v -> dropdown.showDropDown());

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.saveBtn).setOnClickListener(v -> {
            saveProfileUpdate(withGoal(currentUser, selectedGoal[0]));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveProfileUpdate(User updated) {
        userProfilesRepository.updateProfile(updated, new RepoCallback<User>() {
            @Override
            public void onSuccess(User result) {
                runOnUi(() -> {
                    currentUser = result;
                    renderUser(result);
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUi(() -> Toast.makeText(requireContext(),
                        "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private User withHeight(User u, BigDecimal newHeight) {
        return new User(u.getUserId(), u.getName(), newHeight, u.getBirthday(), u.getGender(),
                u.getActivityMultiplier(), u.getCurrentGoal(), u.getTimezone(), u.getDeletionRequestedAt());
    }

    private User withBirthday(User u, LocalDate newBirthday) {
        return new User(u.getUserId(), u.getName(), u.getHeight(), newBirthday, u.getGender(),
                u.getActivityMultiplier(), u.getCurrentGoal(), u.getTimezone(), u.getDeletionRequestedAt());
    }

    private User withActivity(User u, BigDecimal newMultiplier) {
        return new User(u.getUserId(), u.getName(), u.getHeight(), u.getBirthday(), u.getGender(),
                newMultiplier, u.getCurrentGoal(), u.getTimezone(), u.getDeletionRequestedAt());
    }

    private User withGoal(User u, String newGoal) {
        return new User(u.getUserId(), u.getName(), u.getHeight(), u.getBirthday(), u.getGender(),
                u.getActivityMultiplier(), newGoal, u.getTimezone(), u.getDeletionRequestedAt());
    }

    private void confirmLogOut() {
        View sheetView = inflateSheet(R.layout.bottom_dialog_confirm);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.confirmTitle);
        TextView message = sheetView.findViewById(R.id.confirmMessage);
        title.setText(R.string.log_out);
        message.setText(R.string.log_out_confirm_message);

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.confirmBtn).setOnClickListener(v -> {
            dialog.dismiss();
            doLogOut();
        });

        dialog.show();
    }

    private void doLogOut() {
        authRepository.signOut(new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                runOnUi(AccountFragment.this::navigateToLogin);
            }

            @Override
            public void onError(Exception e) {
                runOnUi(AccountFragment.this::navigateToLogin);
            }
        });
    }

    private void confirmDeleteAccount() {
        View sheetView = inflateSheet(R.layout.bottom_dialog_confirm);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.confirmTitle);
        TextView message = sheetView.findViewById(R.id.confirmMessage);
        title.setText(R.string.delete_account);
        message.setText(R.string.delete_account_confirm_message);

        sheetView.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.confirmBtn).setOnClickListener(v -> {
            dialog.dismiss();
            doDeleteAccount();
        });

        dialog.show();
    }

    private void doDeleteAccount() {
        userProfilesRepository.flagForDeletion(new RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUi(() -> {
                    Toast.makeText(requireContext(), "Account scheduled for deletion", Toast.LENGTH_LONG).show();
                    doLogOut();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUi(() -> Toast.makeText(requireContext(),
                        "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void runOnUi(Runnable action) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(action);
    }
}