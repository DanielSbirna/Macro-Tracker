package com.example.macrotracker;

import android.app.Service;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.models.Meal;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.example.macrotracker.ui.adapters.RecentMealsAdapter;
import com.example.macrotracker.MainActivity;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.MealType;
import com.example.macrotracker.util.JwtUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;


public class AddDrawerFragment extends BottomSheetDialogFragment {

    // result contract so other fragments know when to refresh
    public static final String RESULT_KEY = "add_drawer_result";
    public static final String RESULT_MEAL_ADDED = "meal_added";

    private ViewFlipper flipper;
    private MaterialButton selectedMealTypeButton;
    private Meal selectedMeal;
    private boolean mealAdded = false;

    private static final int PAGE_RECENT_ITEMS = 0;
    private static final int PAGE_MEAL_TYPE = 1;
    private static final int RECENT_MEALS_LIMIT = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_drawer_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        flipper = view.findViewById(R.id.addDrawerFlipper);

        View recentItemsPage = view.findViewById(R.id.pageRecentMeals);
        View mealTypePage = view.findViewById(R.id.pageMealType);

        setupRecentItemsPage(recentItemsPage);
        setupMealTypePage(mealTypePage);
    }

    // Page 1 Recent Items

    private void setupRecentItemsPage(View page) {
        ImageButton cancelBtn = page.findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(v -> dismiss());

        View cardNew = page.findViewById(R.id.cardNew);
        cardNew.setOnClickListener(v -> {
            dismiss();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToAssistant();
            }
        });

        RecyclerView recentList = page.findViewById(R.id.recentItemsList);
        if (recentList != null) {
            recentList.setLayoutManager(new LinearLayoutManager(requireContext()));

            RecentMealsAdapter adapter = new RecentMealsAdapter(new ArrayList<>(), meal -> {
                selectedMeal = meal;
                showPage(PAGE_MEAL_TYPE);
            });
            recentList.setAdapter(adapter);

            // CHANGED: entire block is new — fetches real recent meals from mealLogRepository instead of using fake data
            ServiceLocator.getInstance(requireContext()).mealLogRepository
                    .getRecentMeals(RECENT_MEALS_LIMIT, new RepoCallback<List<Meal>>() {
                        @Override
                        public void onSuccess(List<Meal> meals) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                adapter.updateMeals(meals);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Couldn't load recent meals", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        }
    }

    // Page 2 Meal Type

    private void setupMealTypePage(View page) {
        ImageButton cancelBtn = page.findViewById(R.id.cancelBtnStep2);
        cancelBtn.setOnClickListener(v -> dismiss());

        MaterialButton breakfastBtn = page.findViewById(R.id.breakfastBtn);
        MaterialButton lunchBtn = page.findViewById(R.id.lunchBtn);
        MaterialButton snackBtn = page.findViewById(R.id.snackBtn);
        MaterialButton dinnerBtn = page.findViewById(R.id.dinnerBtn);

        List<MaterialButton> mealButtons = Arrays.asList(breakfastBtn, lunchBtn, snackBtn, dinnerBtn);
        selectedMealTypeButton = breakfastBtn;
        breakfastBtn.setChecked(true);

        for (MaterialButton button : mealButtons) {
            button.setOnClickListener(v -> selectMealType(button, mealButtons));
        }

        MaterialButton btnAddMeal = page.findViewById(R.id.addBtn);
        btnAddMeal.setOnClickListener(v -> onAddMealClicked());
    }

    private void onAddMealClicked() {
        if (selectedMeal == null) {
            Toast.makeText(requireContext(), "No meal selected", Toast.LENGTH_SHORT).show();
            return;
        }

        MealType type = buttonToMealType (selectedMealTypeButton);

        String accessToken = ServiceLocator.getInstance(requireContext()).tokenStorage.getAccessToken();
        String userId;
        try {
            userId = JwtUtils.getUserIdFromToken(accessToken);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "You are not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Meal newMeal = new Meal(
                userId,
                selectedMeal.getTitle(),
                type,
                selectedMeal.getCalories(),
                selectedMeal.getProtein(),
                selectedMeal.getCarbs(),
                selectedMeal.getFats(),
                OffsetDateTime.now()
        );

        ServiceLocator.getInstance(requireContext()).mealLogRepository
                .insertMeal(newMeal, new RepoCallback<Meal>() {
                    @Override
                    public void onSuccess(Meal inserted) {
                        if (!isAdded()) return;
                        mealAdded = true;
                        requireActivity().runOnUiThread(AddDrawerFragment.this::dismiss);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Couldn't add meal", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    // fire the result once the sheet closes
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        Bundle result = new Bundle();
        result.putBoolean(RESULT_MEAL_ADDED, mealAdded);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }

    private void selectMealType(MaterialButton selected, List<MaterialButton> allButtons) {
        for (MaterialButton button : allButtons) {
            button.setChecked(button == selected);
        }
        selectedMealTypeButton = selected;
    }
    
    private MealType buttonToMealType(MaterialButton button) {
        int id = button.getId();
        if (id == R.id.breakfastBtn) return MealType.BREAKFAST;
        if (id == R.id.lunchBtn) return MealType.LUNCH;
        if (id == R.id.snackBtn) return MealType.SNACKS;
        if (id == R.id.dinnerBtn) return MealType.DINNER;
        throw new IllegalArgumentException("Unknown meal type button");
    }

    // Shared
    private void showPage(int pageIndex) {
        flipper.setDisplayedChild(pageIndex);
    }
}