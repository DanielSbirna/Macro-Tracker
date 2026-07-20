package com.example.macrotracker.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.R;
import com.example.macrotracker.models.Meal;

import java.util.List;

public class RecentMealsAdapter extends RecyclerView.Adapter<RecentMealsAdapter.MealViewHolder> {

    public interface OnMealClickListener {
        void onMealClick(Meal meal);
    }

    private final List<Meal> meals;
    private final OnMealClickListener listener;

    public RecentMealsAdapter(List<Meal> meals, OnMealClickListener listener) {
        this.meals = meals;
        this.listener = listener;
    }

    public void updateMeals(List<Meal> newMeals) {
        meals.clear();
        meals.addAll(newMeals);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = meals.get(position);
        holder.title.setText(meal.getTitle());

        View.OnClickListener click = v -> listener.onMealClick(meal);
        holder.itemView.setOnClickListener(click);
        holder.btnAdd.setOnClickListener(click);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageButton btnAdd;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.mealTitle);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}