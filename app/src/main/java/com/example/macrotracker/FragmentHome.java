package com.example.macrotracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.data.repository.MealLogRepository;
import com.example.macrotracker.data.repository.TargetMacrosRepository;
import com.example.macrotracker.data.repository.UserProfilesRepository;
import com.example.macrotracker.databinding.FragmentHomeBinding;
import com.example.macrotracker.models.GoalType;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.models.User;
import com.example.macrotracker.ui.GreetingHelper;
import com.example.macrotracker.ui.widgets.MacroCardView;
import com.example.macrotracker.ui.widgets.StatColumnView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
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

        ServiceLocator serviceLocator = ServiceLocator.getInstance(requireContext());
        userProfilesRepository = serviceLocator.userProfilesRepository;
        targetMacrosRepository = serviceLocator.targetMacrosRepository;
        mealLogRepository = serviceLocator.mealLogRepository;

        // Trend, Suggestion and statRow gone till they have content
        binding.trendCard.getRoot().setVisibility(View.GONE);
        binding.suggestionCard.getRoot().setVisibility(View.GONE);
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

    private void setupCalendarStrip() {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = buildMonthRange(currentMonth); // e.g. 2 before, 2 after
        binding.calendarStrip.setMonths(months, currentMonth);

        LocalDate today = LocalDate.now();
        List<LocalDate> days = buildWeekDays(today);
        binding.calendarStrip.setDays(days, today);

        binding.calendarStrip.setOnDaySelectedListener(this::onDateSelected);
    }

    private void onDateSelected(LocalDate date) {
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
                                loadMealsForSelectedDate();
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

    private void loadMealsForSelectedDate() {
        if (currentTarget == null) return; // nothing to compare today's totals against yet

        ZoneId zone = (currentUser != null && currentUser.getTimezone() != null)
                ? ZoneId.of(currentUser.getTimezone())
                : ZoneId.systemDefault();

        OffsetDateTime start = selectedDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = selectedDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        mealLogRepository.getMealsBetween(start, end, new RepoCallback<List<Meal>>() {
            @Override
            public void onSuccess(List<Meal> meals) {
                runOnUi(() -> updateMacroWidgets(meals, currentTarget));
            }

            @Override
            public void onError(Exception e) {
                // leave widgets showing the last known totals
            }
        });
    }

    // Rendering

    private void updateTopCard() {
        boolean accountComplete = currentUser != null;

        if (!accountComplete) {
            binding.completeAccLayout.setVisibility(View.VISIBLE);
            binding.goalLayout.setVisibility(View.GONE);
        } else {
            binding.completeAccLayout.setVisibility(View.GONE);
            binding.goalLayout.setVisibility(View.VISIBLE);

            binding.welcomeText.setText(GreetingHelper.getGreeting(currentUser.getName()));

            GoalType goalType = resolveGoalType(currentUser.getCurrentGoal());
            if (goalType != null) {
                binding.goalChangeBtn.setText(goalType.label);
            }
        }
    }

    private void updateMacroWidgets(List<Meal> meals, TargetMacros target) {
        binding.noMacrosText.setVisibility(View.GONE);
        binding.statsRow.setVisibility(View.VISIBLE);

        BigDecimal eatenCalories = BigDecimal.ZERO;
        BigDecimal eatenProtein = BigDecimal.ZERO;
        BigDecimal eatenCarbs = BigDecimal.ZERO;
        BigDecimal eatenFats = BigDecimal.ZERO;

        for (Meal meal : meals) {
            eatenCalories = eatenCalories.add(meal.getCalories());
            eatenProtein = eatenProtein.add(meal.getProtein());
            eatenCarbs = eatenCarbs.add(meal.getCarbs());
            eatenFats = eatenFats.add(meal.getFats());
        }

        // Remaining-calories ring: progress = eaten so far, max = target
        BigDecimal remaining = target.getCalories().subtract(eatenCalories);
        binding.remainingCaloriesRing.setMax(target.getCalories().floatValue());
        binding.remainingCaloriesRing.setProgress(eatenCalories.floatValue());
        binding.remainingCaloriesValue.setText(formatWhole(remaining));
        binding.goalCaloriesValue.setText(getString(R.string.of_x_kcal_format, formatWhole(target.getCalories())));

        // Macro cards
        bindMacroCard(binding.proteinCard, eatenProtein, target.getProtein());
        bindMacroCard(binding.carbsCard, eatenCarbs, target.getCarbs());
        bindMacroCard(binding.fatsCard, eatenFats, target.getFats());

        // Stat row
        bindStatColumn(binding.kcalStat, eatenCalories, target.getCalories());
        bindStatColumn(binding.proteinStat, eatenProtein, target.getProtein());
        bindStatColumn(binding.carbsStat, eatenCarbs, target.getCarbs());
        bindStatColumn(binding.fatsStat, eatenFats, target.getFats());
    }

    private void bindMacroCard(MacroCardView card, BigDecimal eaten, BigDecimal target) {
        card.setValue(formatWhole(eaten) + "g / " + formatWhole(target) + "g");
        card.setProgress(percentOf(eaten, target));
    }

    private void bindStatColumn(StatColumnView stat, BigDecimal eaten, BigDecimal target) {
        stat.setValue(formatWhole(eaten));
        stat.setPercent(formatPercent(percentOf(eaten, target)) + "%");
    }

    private String formatPercent(float percent) {
        if (Float.isNaN(percent) || Float.isInfinite(percent)) {
            percent = 0f;
        }
        return String.format(Locale.getDefault(), "%.1f", percent);
    }

    private float percentOf(BigDecimal eaten, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) return 0f;
        return eaten.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .floatValue();
    }

    private String formatWhole(BigDecimal value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
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