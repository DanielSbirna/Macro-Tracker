package com.example.macrotracker.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.R;
import com.example.macrotracker.models.FoodLogRow;
import com.example.macrotracker.models.Meal;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<FoodLogRow> rows = new ArrayList<>();

    @Override
    public int getItemViewType(int position) {
        FoodLogRow row = rows.get(position);
        if (row instanceof FoodLogRow.HeaderRow) return FoodLogRow.TYPE_HEADER;
        return FoodLogRow.TYPE_MEAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case FoodLogRow.TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_meal_header, parent, false));
            default:
                return new MealViewHolder(inflater.inflate(R.layout.item_meal_card, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FoodLogRow row = rows.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((FoodLogRow.HeaderRow) row);
        } else {
            ((MealViewHolder) holder).bind((FoodLogRow.MealRow) row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public void submitRows(List<FoodLogRow> newRows) {
        this.rows = newRows;
        notifyDataSetChanged();
    }

    // ViewHolders

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.mealTypeTitle);
            icon = itemView.findViewById(R.id.mealTypeIcon);
        }

        void bind(FoodLogRow.HeaderRow row) {
            title.setText(row.type.getDisplayName());
            int resId = itemView.getContext().getResources().getIdentifier(
                    row.type.getIconName(), "drawable", itemView.getContext().getPackageName());
            if (resId != 0) {
                icon.setImageResource(resId);
            }
        }
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        private static final DateTimeFormatter TIME_FORMAT =
                DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
        TextView name, time, kcal, macroLine;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.mealTitle);
            time = itemView.findViewById(R.id.mealInputTime);
            kcal = itemView.findViewById(R.id.mealKcal);
            macroLine = itemView.findViewById(R.id.mealMacroLine);
        }

        void bind(FoodLogRow.MealRow row) {
            Meal m = row.meal;
            name.setText(m.getTitle());
            time.setText(m.getLoggedAt().atZoneSameInstant(ZoneId.systemDefault()).format(TIME_FORMAT));
            kcal.setText(String.format(Locale.getDefault(), "%d kcal", m.getCalories().intValue()));
            macroLine.setText(String.format(Locale.getDefault(),
                    "P %d | C %d | F %d",
                    m.getProtein().intValue(), m.getCarbs().intValue(), m.getFats().intValue()));
        }
    }
}