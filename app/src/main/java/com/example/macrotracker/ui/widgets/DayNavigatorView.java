package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.macrotracker.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DayNavigatorView  extends ConstraintLayout {

    public interface OnDateChangedListener {
        void onDateChanged(LocalDate date);
    }

    private TextView dayLabel, dateLabel;
    private LocalDate currentDate;
    private OnDateChangedListener listener;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault());

    public DayNavigatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.bg_bordered_container_subtle);
        LayoutInflater.from(context).inflate(R.layout.day_navigator, this, true);

        dayLabel = findViewById(R.id.dayLabel);
        dateLabel = findViewById(R.id.dateLabel);

        ImageButton prev = findViewById(R.id.dayPrevBtn);
        ImageButton next = findViewById(R.id.dayNextBtn);

        prev.setOnClickListener(v -> shiftDay(-1));
        next.setOnClickListener(v -> shiftDay(1));

        setDate(LocalDate.now());
    }

    public void setDate(LocalDate date) {
        currentDate = date;
        dayLabel.setText(labelForDay(date));
        dateLabel.setText(DATE_FORMATTER.format(date).toUpperCase(Locale.getDefault()));
    }

    public LocalDate getDate() {
        return currentDate;
    }

    public void setOnDateChangedListener(OnDateChangedListener l) {
        this.listener = l;
    }

    private void shiftDay(int delta) {
        setDate(currentDate.plusDays(delta));
        if (listener != null) listener.onDateChanged(currentDate);
    }

    private String labelForDay(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isEqual(today)) return "TODAY";
        if (date.isEqual(today.minusDays(1))) return "YESTERDAY";
        if (date.isEqual(today.plusDays(1))) return "TOMORROW";
        return date.getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                .toUpperCase(Locale.getDefault());
    }

}
