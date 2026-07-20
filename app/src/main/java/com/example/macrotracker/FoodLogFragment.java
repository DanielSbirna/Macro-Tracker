package com.example.macrotracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.data.repository.MealLogRepository;
import com.example.macrotracker.data.repository.TargetMacrosRepository;
import com.example.macrotracker.models.FoodLogRow;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.MealType;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.ui.SettingsBottomDialog;
import com.example.macrotracker.ui.adapters.FoodLogAdapter;
import com.example.macrotracker.ui.widgets.DayNavigatorView;
import com.example.macrotracker.ui.widgets.LinearProgressView;
import com.example.macrotracker.util.MacroMath;
import com.example.macrotracker.util.MacroTotals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoodLogFragment extends Fragment {

    private LocalDate selectedDate = LocalDate.now();
    private DayNavigatorView dayNavigator;
    private FoodLogAdapter adapter;
    private MealLogRepository mealLogRepository;
    private TargetMacrosRepository targetMacrosRepository;
    private TargetMacros currentTarget;

    // view fields promoted from local vars in onViewCreated so bind methods can reach them
    private TextView caloriesValue, caloriesPercent, caloriesTarget;
    private TextView proteinPercent, carbPercent, fatsPercent;
    private LinearProgressView caloriesProgress, proteinProgress, carbProgress, fatsProgress;

    public FoodLogFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_food_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ServiceLocator serviceLocator = ServiceLocator.getInstance(requireContext());
        mealLogRepository = serviceLocator.mealLogRepository;
        targetMacrosRepository = serviceLocator.targetMacrosRepository; // CHANGED

        // CHANGED: refresh the currently viewed day after AddDrawerFragment logs a meal
        getParentFragmentManager().setFragmentResultListener(
                AddDrawerFragment.RESULT_KEY, getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    if (bundle.getBoolean(AddDrawerFragment.RESULT_MEAL_ADDED)) {
                        loadMeals(selectedDate);
                    }
                });

        dayNavigator = view.findViewById(R.id.dayNavigator);
        dayNavigator.setDate(selectedDate);
        dayNavigator.setOnDateChangedListener(this::onDateChanged);

        caloriesValue = view.findViewById(R.id.caloriesValue);
        caloriesProgress = view.findViewById(R.id.caloriesProgress);
        caloriesPercent = view.findViewById(R.id.caloriesPercent);
        caloriesTarget = view.findViewById(R.id.caloriesTarget);
        proteinProgress = view.findViewById(R.id.proteinProgress); // CHANGED: was missing before
        proteinPercent = view.findViewById(R.id.proteinPercent);
        carbProgress = view.findViewById(R.id.carbProgress); // CHANGED: was missing before
        carbPercent = view.findViewById(R.id.carbPercent);
        fatsProgress = view.findViewById(R.id.fatsProgress); // CHANGED: was missing before
        fatsPercent = view.findViewById(R.id.fatsPercent);

        RecyclerView foodLogList = view.findViewById(R.id.foodLogList);
        foodLogList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FoodLogAdapter();
        foodLogList.setAdapter(adapter);

        loadTargetThenMeals(); // CHANGED: was loadMeals(selectedDate) directly

        view.findViewById(R.id.settingsBtn).setOnClickListener(v ->
                new SettingsBottomDialog().show(getParentFragmentManager(), "settings"));
    }

    private void loadTargetThenMeals() {
        targetMacrosRepository.getLatestTarget(new RepoCallback<TargetMacros>() {
            @Override
            public void onSuccess(TargetMacros target) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    currentTarget = target;
                    loadMeals(selectedDate);
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    // No target yet — still show the meal list, just skip bar binding
                    loadMeals(selectedDate);
                });
            }
        });
    }

    private void loadMeals(LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        mealLogRepository.getMealsBetween(startOfDay, endOfDay, new RepoCallback<List<Meal>>() {
            @Override
            public void onSuccess(List<Meal> meals) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    List<FoodLogRow> rows = buildRows(meals);
                    adapter.submitRows(rows);
                    bindMacroBars(meals);
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    adapter.submitRows(new ArrayList<>());
                });
            }
        });
    }

    private void bindMacroBars(List<Meal> meals) {
        if (currentTarget == null) return; // nothing to compare against yet

        MacroTotals totals = MacroTotals.sum(meals);

        caloriesProgress.setMax(currentTarget.getCalories().floatValue());
        caloriesProgress.setProgress(totals.calories.floatValue());
        caloriesValue.setText(MacroMath.formatWhole(currentTarget.getCalories().subtract(totals.calories)));
        caloriesPercent.setText(MacroMath.formatPercent(MacroMath.percentOf(totals.calories, currentTarget.getCalories())) + "%");
        caloriesTarget.setText(MacroMath.formatWhole(currentTarget.getCalories()) + " kcal");

        proteinProgress.setMax(currentTarget.getProtein().floatValue());
        proteinProgress.setProgress(totals.protein.floatValue());
        proteinPercent.setText(MacroMath.formatPercent(MacroMath.percentOf(totals.protein, currentTarget.getProtein())) + "%");

        carbProgress.setMax(currentTarget.getCarbs().floatValue());
        carbProgress.setProgress(totals.carbs.floatValue());
        carbPercent.setText(MacroMath.formatPercent(MacroMath.percentOf(totals.carbs, currentTarget.getCarbs())) + "%");

        fatsProgress.setMax(currentTarget.getFats().floatValue());
        fatsProgress.setProgress(totals.fats.floatValue());
        fatsPercent.setText(MacroMath.formatPercent(MacroMath.percentOf(totals.fats, currentTarget.getFats())) + "%");
    }

    private List<FoodLogRow> buildRows(List<Meal> meals) {
        List<FoodLogRow> rows = new ArrayList<>();
        Map<MealType, List<Meal>> grouped = meals.stream()
                .collect(Collectors.groupingBy(Meal::getMealType, () -> new EnumMap<>(MealType.class), Collectors.toList()));

        for (MealType type : MealType.values()) {
            List<Meal> group = grouped.getOrDefault(type, Collections.emptyList());
            if (group.isEmpty()) continue; // no empty containers - skip sections with nothing logged

            rows.add(new FoodLogRow.HeaderRow(type));
            for (int i = 0; i < group.size(); i++) {
                FoodLogRow.CardPosition pos;
                if (group.size() == 1) {
                    pos = FoodLogRow.CardPosition.SINGLE;
                } else if (i == 0) {
                    pos = FoodLogRow.CardPosition.TOP;
                } else if (i == group.size() - 1) {
                    pos = FoodLogRow.CardPosition.BOTTOM;
                } else {
                    pos = FoodLogRow.CardPosition.MIDDLE;
                }
                rows.add(new FoodLogRow.MealRow(group.get(i), pos));
            }
        }
        return rows;
    }

    private void onDateChanged(LocalDate date) {
        if (date.equals(selectedDate)) return;
        selectedDate = date;
        loadMeals(selectedDate);
    }
}