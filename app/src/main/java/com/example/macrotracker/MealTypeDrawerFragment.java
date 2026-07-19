package com.example.macrotracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.macrotracker.R;
import com.example.macrotracker.models.MealType;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

public class MealTypeDrawerFragment extends BottomSheetDialogFragment {

    public static final String RESULT_KEY = "meal_type_drawer_result";
    public static final String RESULT_MEAL_TYPE = "meal_type";
    public static final String RESULT_CONFIRMED = "confirmed";

    private static final String ARG_DEFAULT_TYPE = "arg_default_type";

    private MealType selectedType;
    private boolean confirmed = false;

    public static MealTypeDrawerFragment newInstance(MealType defaultType) {
        MealTypeDrawerFragment fragment = new MealTypeDrawerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEFAULT_TYPE, defaultType.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_drawer_meal_type, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String defaultTypeName = requireArguments().getString(ARG_DEFAULT_TYPE);
        selectedType = MealType.valueOf(defaultTypeName);

        ImageButton cancelBtn = view.findViewById(R.id.cancelBtnStep2);
        cancelBtn.setOnClickListener(v -> dismiss());

        MaterialButton breakfastBtn = view.findViewById(R.id.breakfastBtn);
        MaterialButton lunchBtn = view.findViewById(R.id.lunchBtn);
        MaterialButton snackBtn = view.findViewById(R.id.snackBtn);
        MaterialButton dinnerBtn = view.findViewById(R.id.dinnerBtn);

        List<MaterialButton> mealButtons = Arrays.asList(breakfastBtn, lunchBtn, snackBtn, dinnerBtn);

        // Override the XML's hardcoded "Breakfast checked" default with whatever
        // was actually passed in (usually computed from the current time of day).
        for (MaterialButton button : mealButtons) {
            button.setChecked(buttonMatchesType(button, selectedType));
            button.setOnClickListener(v -> {
                for (MaterialButton b : mealButtons) {
                    b.setChecked(b == button);
                }
                selectedType = buttonToMealType(button);
            });
        }

        MaterialButton btnAddMeal = view.findViewById(R.id.addBtn);
        btnAddMeal.setOnClickListener(v -> {
            confirmed = true;
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        Bundle result = new Bundle();
        result.putBoolean(RESULT_CONFIRMED, confirmed);
        result.putString(RESULT_MEAL_TYPE, selectedType.name());
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }

    private boolean buttonMatchesType(MaterialButton button, MealType type) {
        return buttonToMealType(button) == type;
    }

    private MealType buttonToMealType(MaterialButton button) {
        int id = button.getId();
        if (id == R.id.breakfastBtn) return MealType.BREAKFAST;
        if (id == R.id.lunchBtn) return MealType.LUNCH;
        if (id == R.id.snackBtn) return MealType.SNACKS;
        if (id == R.id.dinnerBtn) return MealType.DINNER;
        throw new IllegalArgumentException("Unknown meal type button");
    }
}
