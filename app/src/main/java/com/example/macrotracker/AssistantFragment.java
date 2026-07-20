package com.example.macrotracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.data.repository.MealLogRepository;
import com.example.macrotracker.data.repository.SuggestionRepository;
import com.example.macrotracker.data.repository.TargetMacrosRepository;
import com.example.macrotracker.models.MacroEstimate;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.MealType;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.MealTypeDrawerFragment;
import com.example.macrotracker.ui.widgets.LinearProgressView;
import com.example.macrotracker.util.JwtUtils;
import com.example.macrotracker.util.MacroMath;
import com.example.macrotracker.util.MacroTotals;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

public class AssistantFragment extends Fragment {

    private EditText mealInput;
    private MaterialButton getEstimateBtn;

    private View resultLayout;
    private LinearProgressView caloriesProgress, proteinProgress, carbProgress, fatsProgress;
    private TextView caloriesPercent, caloriesTarget;
    private TextView proteinPercent, proteinTarget;
    private TextView carbPercent, carbsTarget;
    private TextView fatsPercent, fatsTarget;

    private View suggestionCard;
    private TextView suggestionBody;

    private SuggestionRepository suggestionRepository;
    private MealLogRepository mealLogRepository;
    private TargetMacrosRepository targetMacrosRepository;

    // Held between "estimate rendered" and "meal type confirmed", since the
    // drawer is a separate fragment/round-trip.
    private MacroEstimate pendingEstimate;

    public AssistantFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServiceLocator serviceLocator = ServiceLocator.getInstance(requireContext());
        suggestionRepository = serviceLocator.suggestionRepository;
        mealLogRepository = serviceLocator.mealLogRepository;
        targetMacrosRepository = serviceLocator.targetMacrosRepository;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mealInput = view.findViewById(R.id.inputMealField);
        getEstimateBtn = view.findViewById(R.id.getEstimateBtn);

        resultLayout = view.findViewById(R.id.resultLayout);
        caloriesProgress = view.findViewById(R.id.caloriesProgress);
        caloriesPercent = view.findViewById(R.id.caloriesPercent);
        caloriesTarget = view.findViewById(R.id.caloriesTarget);
        proteinProgress = view.findViewById(R.id.proteinProgress);
        proteinPercent = view.findViewById(R.id.proteinPercent);
        proteinTarget = view.findViewById(R.id.proteinTarget);
        carbProgress = view.findViewById(R.id.carbProgress);
        carbPercent = view.findViewById(R.id.carbPercent);
        carbsTarget = view.findViewById(R.id.carbsTarget);
        fatsProgress = view.findViewById(R.id.fatsProgress);
        fatsPercent = view.findViewById(R.id.fatsPercent);
        fatsTarget = view.findViewById(R.id.fatsTarget);

        suggestionCard = view.findViewById(R.id.suggestionCard);
        suggestionBody = suggestionCard.findViewById(R.id.suggestionBody);

        // Result card is empty until the first successful estimate
        resultLayout.setVisibility(View.GONE);
        suggestionCard.setVisibility(View.GONE);

        getChildFragmentManager().setFragmentResultListener(
                MealTypeDrawerFragment.RESULT_KEY, this, (requestKey, bundle) -> {
                    boolean confirmed = bundle.getBoolean(MealTypeDrawerFragment.RESULT_CONFIRMED);
                    if (!confirmed) {
                        // User backed out via the drawer's X — estimate stays visible,
                        // but nothing gets logged. Let them retry.
                        setLoading(false);
                        return;
                    }
                    MealType chosenType = MealType.valueOf(bundle.getString(MealTypeDrawerFragment.RESULT_MEAL_TYPE));
                    logEstimatedMeal(pendingEstimate, chosenType);
                });

        getEstimateBtn.setOnClickListener(v -> onGetEstimateClicked());
    }

    private void onGetEstimateClicked() {
        String description = mealInput.getText().toString().trim();
        if (description.isEmpty()) {
            mealInput.setError("Describe what you ate first");
            return;
        }

        setLoading(true);

        targetMacrosRepository.getLatestTarget(new RepoCallback<TargetMacros>() {
            @Override
            public void onSuccess(TargetMacros target) {
                fetchTodaysTotalsThenEstimate(description, target);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Couldn't load your target macros: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void fetchTodaysTotalsThenEstimate(String description, TargetMacros target) {
        OffsetDateTime startOfToday = OffsetDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime now = OffsetDateTime.now();

        mealLogRepository.getMealsBetween(startOfToday, now, new RepoCallback<List<Meal>>() {
            @Override
            public void onSuccess(List<Meal> todaysMeals) {
                MacroTotals totals = MacroTotals.sum(todaysMeals); // CHANGED: was a manual for-loop over caloriesSoFar/proteinSoFar/etc.
                requestEstimate(description, target, totals.calories, totals.protein, totals.carbs, totals.fats);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Couldn't load today's totals: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void requestEstimate(String description, TargetMacros target,
                                 BigDecimal caloriesSoFar, BigDecimal proteinSoFar,
                                 BigDecimal carbsSoFar, BigDecimal fatsSoFar) {
        suggestionRepository.estimateMealWithSuggestion(
                description, caloriesSoFar, proteinSoFar, carbsSoFar, fatsSoFar, target,
                new RepoCallback<MacroEstimate>() {
                    @Override
                    public void onSuccess(MacroEstimate estimate) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            renderEstimate(estimate, target, caloriesSoFar, proteinSoFar, carbsSoFar, fatsSoFar);

                            pendingEstimate = estimate;
                            MealTypeDrawerFragment.newInstance(getDefaultMealTypeForNow())
                                    .show(getChildFragmentManager(), "meal_type_drawer");
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(requireContext(),
                                    "Couldn't get an estimate: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void logEstimatedMeal(MacroEstimate estimate, MealType mealType) {
        String userId;
        try {
            userId = JwtUtils.getUserIdFromToken(
                    ServiceLocator.getInstance(requireContext()).tokenStorage.getAccessToken());
        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        Meal meal;
        try {
            meal = new Meal(
                    userId,
                    estimate.getTitle(),
                    mealType,
                    estimate.getCalories(),
                    estimate.getProtein(),
                    estimate.getCarbs(),
                    estimate.getFats(),
                    OffsetDateTime.now());
        } catch (IllegalArgumentException e) {
            // e.g. Gemini returned a negative number, or title > 100 chars
            setLoading(false);
            Toast.makeText(requireContext(), "Couldn't log meal: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        mealLogRepository.insertMeal(meal, new RepoCallback<Meal>() {
            @Override
            public void onSuccess(Meal savedMeal) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Logged!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Estimate shown, but saving failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Rough defaults; the drawer always lets the user override this.
    private MealType getDefaultMealTypeForNow() {
        LocalTime now = LocalTime.now();
        if (!now.isBefore(LocalTime.of(5, 0)) && now.isBefore(LocalTime.of(11, 0))) {
            return MealType.BREAKFAST;
        } else if (!now.isBefore(LocalTime.of(11, 0)) && now.isBefore(LocalTime.of(15, 0))) {
            return MealType.LUNCH;
        } else if (!now.isBefore(LocalTime.of(15, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            return MealType.SNACKS;
        } else if (!now.isBefore(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(22, 0))) {
            return MealType.DINNER;
        }
        return MealType.SNACKS; // late night default
    }

    private void renderEstimate(MacroEstimate estimate, TargetMacros target,
                                BigDecimal caloriesSoFar, BigDecimal proteinSoFar,
                                BigDecimal carbsSoFar, BigDecimal fatsSoFar) {

        BigDecimal newCalories = caloriesSoFar.add(estimate.getCalories());
        BigDecimal newProtein = proteinSoFar.add(estimate.getProtein());
        BigDecimal newCarbs = carbsSoFar.add(estimate.getCarbs());
        BigDecimal newFats = fatsSoFar.add(estimate.getFats());

        bindBar(caloriesProgress, caloriesPercent, caloriesTarget,
                newCalories, target.getCalories(), "kcal");
        bindBar(proteinProgress, proteinPercent, proteinTarget,
                newProtein, target.getProtein(), "g");
        bindBar(carbProgress, carbPercent, carbsTarget,
                newCarbs, target.getCarbs(), "g");
        bindBar(fatsProgress, fatsPercent, fatsTarget,
                newFats, target.getFats(), "g");

        suggestionBody.setText(estimate.getSuggestion());

        resultLayout.setVisibility(View.VISIBLE);
        suggestionCard.setVisibility(View.VISIBLE);
        // setLoading(false) happens once logEstimatedMeal() resolves, not here —
        // button stays disabled until the meal is actually saved.
    }

    private void bindBar(LinearProgressView progressView, TextView percentView, TextView targetView,
                         BigDecimal value, BigDecimal max, String unit) {
        float valueF = value.floatValue();
        float maxF = max.floatValue();

        progressView.setMax(maxF);
        progressView.setProgress(valueF);

        float percent = MacroMath.percentOf(value, max);
        percentView.setText(MacroMath.formatPercent(percent) + "%");

        targetView.setText(MacroMath.formatWhole(max) + " " + unit);
    }

    private void setLoading(boolean loading) {
        getEstimateBtn.setEnabled(!loading);
        getEstimateBtn.setText(loading ? "Estimating…" : getString(R.string.get_estimate));
    }
}
