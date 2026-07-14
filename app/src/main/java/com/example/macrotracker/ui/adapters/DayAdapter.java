package com.example.macrotracker.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final List<LocalDate> days;
    private LocalDate selectedDate;
    private final OnDayClick onDayClick;

    public interface OnDayClick { void onClick(LocalDate date); }

    public DayAdapter(List<LocalDate> days, LocalDate selected, OnDayClick onDayClick) {
        this.days = days;
        this.selectedDate = selected;
        this.onDayClick = onDayClick;
    }

    public void setSelected(LocalDate date) {
        this.selectedDate = date;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = days.get(position);
        boolean isSelected = date.equals(selectedDate);

        holder.dayLabel.setText(date.format(DateTimeFormatter.ofPattern("EEE")));
        holder.dayNumber.setText(String.valueOf(date.getDayOfMonth()));
        holder.itemView.setSelected(isSelected);

        int textColor = isSelected
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.white)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_gray_text);

        int labelColor = isSelected
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.white)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_gray_text);

        holder.dayNumber.setTextColor(textColor);
        holder.dayLabel.setTextColor(labelColor);

        holder.itemView.setOnClickListener(v -> onDayClick.onClick(date));
    }

    @Override
    public int getItemCount() { return days.size(); }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayLabel, dayNumber;
        DayViewHolder(View itemView) {
            super(itemView);
            dayLabel = itemView.findViewById(R.id.dayLabel);
            dayNumber = itemView.findViewById(R.id.dayNumber);
        }
    }
}