package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.macrotracker.ui.adapters.DayAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.R;
import com.example.macrotracker.ui.adapters.MonthAdapter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class CalendarStripView extends LinearLayout {

    public interface OnDaySelectedListener {
        void onDaySelected(LocalDate date);
    }

    private RecyclerView monthRecycler, dayRecycler;
    private DayAdapter dayAdapter;
    private MonthAdapter monthAdapter;
    private OnDaySelectedListener listener;

    public CalendarStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_bordered_container_subtle);
        LayoutInflater.from(context).inflate(R.layout.calendar_strip, this, true);

        monthRecycler = findViewById(R.id.monthRecycler);
        dayRecycler = findViewById(R.id.dayRecycler);

        monthRecycler.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        dayRecycler.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        int daySpacing = dpToPx(8);
        dayRecycler.addItemDecoration(new HorizontalSpacingDecoration(daySpacing));

        int monthSpacing = dpToPx(16);
        monthRecycler.addItemDecoration(new HorizontalSpacingDecoration(monthSpacing));

        ImageButton prev = findViewById(R.id.monthPrevBtn);
        ImageButton next = findViewById(R.id.monthNextBtn);
        prev.setOnClickListener(v -> shiftMonth(-1));
        next.setOnClickListener(v -> shiftMonth(1));
    }

    public void setDays(List<LocalDate> days, LocalDate selected) {
        dayAdapter = new DayAdapter(days, selected, date -> {
            dayAdapter.setSelected(date);
            if (listener != null) listener.onDaySelected(date);
        });
        dayRecycler.setAdapter(dayAdapter);
    }

    public void setMonths(List<YearMonth> months, YearMonth selected) {
        monthAdapter = new MonthAdapter(months, selected, month -> {

        });
        monthRecycler.setAdapter(monthAdapter);
    }

    public void setOnDaySelectedListener(OnDaySelectedListener l) {
        this.listener = l;
    }

    private void shiftMonth(int delta) {

    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static class HorizontalSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        HorizontalSpacingDecoration(int spacing) {
            this.spacing = spacing;
        }

        // Even spacing
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position != 0) {
                outRect.left = spacing;
            }
        }
    }

}
