package com.example.macrotracker.ui;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.macrotracker.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

public class CompleteAccountBottomDialog extends BottomSheetDialogFragment {

    public interface OnAccountCompletedListener {
        void onAccountCompleted(AccountDetails details, Runnable onSuccess, Runnable onFailure);
    }

    public static class AccountDetails {
        public final String name;
        public final String birthday; // ISO format: yyyy-MM-dd
        public final float weightKg;
        public final float heightCm;
        public final String gender;
        public final String activityLevel;

        public AccountDetails(String name, String birthday, float weightKg, float heightCm,
                              String gender, String activityLevel) {
            this.name = name;
            this.birthday = birthday;
            this.weightKg = weightKg;
            this.heightCm = heightCm;
            this.gender = gender;
            this.activityLevel = activityLevel;
        }

        public static int calculateAge(String isoBirthday) {
            String[] parts = isoBirthday.split("-");
            Calendar birth = Calendar.getInstance();
            birth.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        }
    }

    private OnAccountCompletedListener listener;
    private String selectedBirthdayIso; // null until picked

    public void setOnAccountCompletedListener(OnAccountCompletedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(d -> {
            View sheet = ((BottomSheetDialog) d).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setHideable(false);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_complete_account_bottom_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText nameInput = view.findViewById(R.id.nameInput);
        EditText birthdayInput = view.findViewById(R.id.birthdayInput);
        EditText weightInput = view.findViewById(R.id.weightInput);
        EditText heightInput = view.findViewById(R.id.heightInput);
        Spinner genderSpinner = view.findViewById(R.id.genderSpinner);
        Spinner activitySpinner = view.findViewById(R.id.activitySpinner);
        MaterialButton saveBtn = view.findViewById(R.id.saveBtn);

        genderSpinner.setAdapter(ArrayAdapter.createFromResource(
                requireContext(), R.array.genders, android.R.layout.simple_spinner_item));
        activitySpinner.setAdapter(ArrayAdapter.createFromResource(
                requireContext(), R.array.activity_levels, android.R.layout.simple_spinner_item));

        birthdayInput.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            Calendar initial = Calendar.getInstance();
            initial.add(Calendar.YEAR, -18); // reasonable default starting point

            DatePickerDialog picker = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        selectedBirthdayIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        birthdayInput.setText(String.format(Locale.US, "%02d/%02d/%04d", month + 1, dayOfMonth, year));
                    },
                    initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));

            picker.getDatePicker().setMaxDate(now.getTimeInMillis()); // no future birthdays
            picker.show();
        });

        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String weightStr = weightInput.getText().toString().trim();
            String heightStr = heightInput.getText().toString().trim();

            if (TextUtils.isEmpty(name) || selectedBirthdayIso == null
                    || TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(heightStr)) {
                Toast.makeText(requireContext(), R.string.error_fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            float weight;
            float height;
            try {
                weight = Float.parseFloat(weightStr);
                height = Float.parseFloat(heightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), R.string.error_invalid_number, Toast.LENGTH_SHORT).show();
                return;
            }

            AccountDetails details = new AccountDetails(name, selectedBirthdayIso, weight, height,
                    genderSpinner.getSelectedItem().toString(),
                    activitySpinner.getSelectedItem().toString());

            saveBtn.setEnabled(false);
            if (listener != null) {
                listener.onAccountCompleted(details,
                        () -> {
                            saveBtn.setEnabled(true);
                            dismiss();
                        },
                        () -> {
                            saveBtn.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.error_saving_account, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}