package com.example.macrotracker.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.R;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.MonthViewHolder> {

    public interface OnMonthSelectedListener {
        void onMonthSelected(YearMonth month);
    }

    private final List<YearMonth> months;
    private YearMonth selected;
    private final OnMonthSelectedListener listener;

    public MonthAdapter(List<YearMonth> months, YearMonth selected, OnMonthSelectedListener listener) {
        this.months = new ArrayList<>(months);
        this.selected = selected;
        this.listener = listener;
    }

    public void appendMonths(List<YearMonth> newMonths) {
        int start = months.size();
        months.addAll(newMonths);
        notifyItemRangeInserted(start, newMonths.size());
    }

    public void prependMonths(List<YearMonth> newMonths) {
        months.addAll(0, newMonths);
        notifyItemRangeInserted(0, newMonths.size());
    }

    public void setSelected(YearMonth month) {
        YearMonth previous = selected;
        selected = month;
        int prevIndex = months.indexOf(previous);
        int newIndex = months.indexOf(month);
        if (prevIndex != -1) notifyItemChanged(prevIndex);
        if (newIndex != -1) notifyItemChanged(newIndex);
    }

    public int indexOf(YearMonth month) {
        return months.indexOf(month);
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month, parent, false);
        return new MonthViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        YearMonth month = months.get(position);
        holder.label.setText(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
        holder.label.setSelected(month.equals(selected)); // drives selector state (bold/blue text)

        holder.itemView.setOnClickListener(v -> {
            YearMonth previous = selected;
            selected = month;
            notifyItemChanged(months.indexOf(previous));
            notifyItemChanged(position);
            if (listener != null) listener.onMonthSelected(month);
        });
    }

    @Override
    public int getItemCount() {
        return months.size();
    }
    public YearMonth getMonthAt(int position) { return months.get(position); }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.monthLabel);
        }
    }
}