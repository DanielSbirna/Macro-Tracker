package com.example.macrotracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.data.repository.MealLogRepository;
import com.example.macrotracker.data.repository.TargetMacrosRepository;
import com.example.macrotracker.data.repository.UserProfilesRepository;
import com.example.macrotracker.databinding.FragmentHomeBinding;
import com.example.macrotracker.models.GoalType;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.StreakStatus;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.models.User;
import com.example.macrotracker.ui.GreetingHelper;
import com.example.macrotracker.ui.TrendCardController;
import com.example.macrotracker.ui.widgets.MacroCardView;
import com.example.macrotracker.ui.widgets.StatColumnView;
import com.example.macrotracker.util.MacroMath;
import com.example.macrotracker.util.MacroTotals;
import com.example.macrotracker.util.StreakCalculator;
import com.example.macrotracker.viewmodel.AssistantViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentHome extends Fragment {

    private FragmentHomeBinding binding;

    private UserProfilesRepository userProfilesRepository;
    private TargetMacrosRepository targetMacrosRepository;
    private MealLogRepository mealLogRepository;

    private User currentUser;
    private TargetMacros currentTarget;
    private LocalDate selectedDate = LocalDate.now();

    // suggestion
    private AssistantViewModel uiViewModel;

    // trend
    private TrendCardController trendCardController;

    public FragmentHome() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        trendCardController = new TrendCardController(binding.trendCard, requireContext());

        ServiceLocator serviceLocator = ServiceLocator.getInstance(requireContext());
        userProfilesRepository = serviceLocator.userProfilesRepository;
        targetMacrosRepository = serviceLocator.targetMacrosRepository;
        mealLogRepository = serviceLocator.mealLogRepository;

        getParentFragmentManager().setFragmentResultListener(
                AddDrawerFragment.RESULT_KEY, getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    if (bundle.getBoolean(AddDrawerFragment.RESULT_MEAL_ADDED)) {
                        loadTodayRing();
                        loadMealsForSelectedDate();
                        loadStreakAndTrend();
                    }
                });



        uiViewModel = new ViewModelProvider(requireActivity()).get(AssistantViewModel.class);
        updateSuggestionCard();

        binding.statsRow.setVisibility(View.GONE);

        // Show no macros text
        binding.noMacrosText.setVisibility(View.VISIBLE);

        // no macros show 0 g
        bindMacroCard(binding.proteinCard, BigDecimal.ZERO, BigDecimal.ZERO);
        bindMacroCard(binding.carbsCard, BigDecimal.ZERO, BigDecimal.ZERO);
        bindMacroCard(binding.fatsCard, BigDecimal.ZERO, BigDecimal.ZERO);

        bindGoalChangeButton();
        setupCalendarStrip();
        loadProfileAndTarget();
    }

    private void updateSuggestionCard() {
        if (uiViewModel.lastEstimate != null) {
            TextView suggestionBody = binding.suggestionCard.getRoot().findViewById(R.id.suggestionBody);
            suggestionBody.setText(uiViewModel.lastEstimate.getSuggestion());
            binding.suggestionCard.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.suggestionCard.getRoot().setVisibility(View.GONE);
        }
    }

    private void setupCalendarStrip() {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = buildMonthRange(currentMonth);
        binding.calendarStrip.setMonths(months, currentMonth);

        LocalDate today = LocalDate.now();
        List<LocalDate> days = buildWeekDays(today);
        binding.calendarStrip.setDays(days, today);
        // circular views are pinned for today
        binding.calendarStrip.setOnDaySelectedListener(this::onDaySelected);
    }

    private void onDaySelected(LocalDate date) {
        if (date.equals(selectedDate)) return;
        selectedDate = date;
        loadMealsForSelectedDate();
    }

    private List<YearMonth> buildMonthRange(YearMonth anchor) {
        List<YearMonth> months = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            months.add(anchor.plusMonths(i));
        }
        return months;
    }

    private List<LocalDate> buildWeekDays(LocalDate anchor) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate start = anchor.minusDays(3); // 3 before + today + 3 after = 7 days
        for (int i = 0; i < 7; i++) {
            days.add(start.plusDays(i));
        }
        return days;
    }

    // Data loading
    private void runOnUi(Runnable action) {
        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (binding == null) return;
            action.run();
        });
    }

    private void loadProfileAndTarget() {
        userProfilesRepository.getProfile(new RepoCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUi(() -> {
                    currentUser = user;
                    updateTopCard();

                    if (user == null) return;

                    targetMacrosRepository.getLatestTarget(new RepoCallback<TargetMacros>() {
                        @Override
                        public void onSuccess(TargetMacros target) {
                            runOnUi(() -> {
                                currentTarget = target;
                                loadTodayRing();
                                loadMealsForSelectedDate();
                                loadStreakAndTrend();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUi(() -> {
                    currentUser = null;
                    updateTopCard();
                });
            }
        });
    }

    private void loadTodayRing() {
        if (currentTarget == null) return;

        ZoneId zone = (currentUser != null && currentUser.getTimezone() != null)
                ? ZoneId.of(currentUser.getTimezone())
                : ZoneId.systemDefault();

        LocalDate today = LocalDate.now(zone);
        OffsetDateTime start = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        mealLogRepository.getMealsBetween(start, end, new RepoCallback<List<Meal>>() {
            @Override
            public void onSuccess(List<Meal> meals) {
                runOnUi(() -> {
                    updateRing(meals, currentTarget);
                    updateMacroCards(meals, currentTarget);
                });
            }

            @Override
            public void onError(Exception e) {
                // leave the ring/macro cards showing the last known totals
            }
        });
    }

    private void loadMealsForSelectedDate() {
        if (currentTarget == null) return;

        ZoneId zone = (currentUser != null && currentUser.getTimezone() != null)
                ? ZoneId.of(currentUser.getTimezone())
                : ZoneId.systemDefault();

        OffsetDateTime start = selectedDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = selectedDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        mealLogRepository.getMealsBetween(start, end, new RepoCallback<List<Meal>>() {
            @Override
            public void onSuccess(List<Meal> meals) {
                runOnUi(() -> updateStatsRow(meals, currentTarget));
            }

            @Override
            public void onError(Exception e) {
                // leave the stat row showing the last known totals
            }
        });
    }

    private void loadStreakAndTrend() {
        ZoneId zone = (currentUser != null && currentUser.getTimezone() != null)
                ? ZoneId.of(currentUser.getTimezone())
                : ZoneId.systemDefault();

        LocalDate today = LocalDate.now(zone);

        // fetch past 30 days to compute active streak
        OffsetDateTime start = today.minusDays(30).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        mealLogRepository.getMealsBetween(start, end, new RepoCallback<List<Meal>>() {
            @Override
            public void onSuccess(List<Meal> meals) {
                runOnUi(() -> {
                    StreakCalculator.StreakResult result = StreakCalculator.compute(meals, zone);

                    if (result.hasStreak()) {
                        trendCardController.render(result.getStatus(), result.getStreakDays());
                    } else {
                        trendCardController.hide();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUi(() -> trendCardController.hide());
            }
        });
    }

    // Rendering
    private void updateTopCard() {
        boolean accountComplete = currentUser != null;

        binding.goalLayout.setVisibility(View.VISIBLE);

        binding.welcomeText.setText(GreetingHelper.getGreeting(currentUser.getName()));

        GoalType goalType = resolveGoalType(currentUser.getCurrentGoal());
        if (goalType != null) {
            binding.goalChangeBtn.setText(goalType.label);
        }
    }

    private void updateRing(List<Meal> meals, TargetMacros target) {
        MacroTotals totals = MacroTotals.sum(meals);

        BigDecimal remaining = target.getCalories().subtract(totals.calories);
        binding.remainingCaloriesRing.setMax(target.getCalories().floatValue());
        binding.remainingCaloriesRing.setProgress(totals.calories.floatValue());
        binding.remainingCaloriesValue.setText(MacroMath.formatWhole(remaining));
        binding.goalCaloriesValue.setText(getString(R.string.of_x_kcal_format, MacroMath.formatWhole(target.getCalories())));
    }

    private void updateMacroCards(List<Meal> meals, TargetMacros target) {
        binding.noMacrosText.setVisibility(View.GONE);

        MacroTotals totals = MacroTotals.sum(meals);

        bindMacroCard(binding.proteinCard, totals.protein, target.getProtein());
        bindMacroCard(binding.carbsCard, totals.carbs, target.getCarbs());
        bindMacroCard(binding.fatsCard, totals.fats, target.getFats());
    }

    private void updateStatsRow(List<Meal> meals, TargetMacros target) {
        binding.statsRow.setVisibility(View.VISIBLE);

        MacroTotals totals = MacroTotals.sum(meals);

        bindStatColumn(binding.kcalStat, totals.calories, target.getCalories());
        bindStatColumn(binding.proteinStat, totals.protein, target.getProtein());
        bindStatColumn(binding.carbsStat, totals.carbs, target.getCarbs());
        bindStatColumn(binding.fatsStat, totals.fats, target.getFats());
    }

    private void bindMacroCard(MacroCardView card, BigDecimal eaten, BigDecimal target) {
        card.setValue(MacroMath.formatWhole(eaten) + "g / " + MacroMath.formatWhole(target) + "g");
        card.setProgress(MacroMath.percentOf(eaten, target));
    }

    private void bindStatColumn(StatColumnView stat, BigDecimal eaten, BigDecimal target) {
        stat.setValue(MacroMath.formatWhole(eaten));
        stat.setPercent(MacroMath.formatPercent(MacroMath.percentOf(eaten, target)) + "%");
    }

    private GoalType resolveGoalType(String goal) {
        if (goal == null) return null;
        try {
            return GoalType.valueOf(goal.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void bindGoalChangeButton() {
        binding.goalChangeBtn.setOnClickListener(v -> {

        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // avoid leaking the view
    }
}