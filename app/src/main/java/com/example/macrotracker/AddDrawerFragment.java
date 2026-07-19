package com.example.macrotracker;

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

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.example.macrotracker.ui.adapters.RecentMealsAdapter;

import java.util.Arrays;
import java.util.List;


public class AddDrawerFragment extends BottomSheetDialogFragment {

    private ViewFlipper flipper;
    private MaterialButton selectedMealTypeButton;
    private String selectedMealName;

    private static final int PAGE_RECENT_ITEMS = 0;
    private static final int PAGE_MEAL_TYPE = 1;

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

            List<String> recentMeals = Arrays.asList(
                    "Grilled Chicken Salad", "Oatmeal with Berries", "Turkey Sandwich"
            );

            RecentMealsAdapter adapter = new RecentMealsAdapter(recentMeals, mealName -> {
                selectedMealName = mealName;
                showPage(PAGE_MEAL_TYPE);
            });
            recentList.setAdapter(adapter);
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

        for (MaterialButton button : mealButtons) {
            button.setOnClickListener(v -> selectMealType(button, mealButtons));
        }

        MaterialButton btnAddMeal = page.findViewById(R.id.addBtn);
        btnAddMeal.setOnClickListener(v -> {
            String mealType = selectedMealTypeButton.getText().toString();
            // TODO: hand off (selectedMealName, mealType) to your ViewModel / repository here
            dismiss();
        });
    }

    private void selectMealType(MaterialButton selected, List<MaterialButton> allButtons) {
        for (MaterialButton button : allButtons) {
            button.setChecked(button == selected);
        }
        selectedMealTypeButton = selected;
    }

    // Shared

    private void showPage(int pageIndex) {
        flipper.setDisplayedChild(pageIndex);
    }
}