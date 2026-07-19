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
import com.example.macrotracker.models.FoodLogRow;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.MealType;
import com.example.macrotracker.ui.adapters.FoodLogAdapter;
import com.example.macrotracker.ui.widgets.DayNavigatorView;
import com.example.macrotracker.ui.widgets.LinearProgressView;

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

        mealLogRepository = ServiceLocator.getInstance(requireContext()).mealLogRepository;

        dayNavigator = view.findViewById(R.id.dayNavigator);
        dayNavigator.setDate(selectedDate);
        dayNavigator.setOnDateChangedListener(this::onDateChanged);

        TextView caloriesValue = view.findViewById(R.id.caloriesValue);
        LinearProgressView caloriesProgress = view.findViewById(R.id.caloriesProgress);
        TextView caloriesPercent = view.findViewById(R.id.caloriesPercent);
        TextView caloriesTarget = view.findViewById(R.id.caloriesTarget);
        TextView proteinPercent = view.findViewById(R.id.proteinPercent);
        TextView carbPercent = view.findViewById(R.id.carbPercent);
        TextView fatsPercent = view.findViewById(R.id.fatsPercent);

        RecyclerView foodLogList = view.findViewById(R.id.foodLogList);
        foodLogList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FoodLogAdapter();
        foodLogList.setAdapter(adapter);

        loadMeals(selectedDate);
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
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    // TODO: surface an error state (snackbar/retry) instead of a silent empty list
                    adapter.submitRows(new ArrayList<>());
                });
            }
        });
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