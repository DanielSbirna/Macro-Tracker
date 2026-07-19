package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.macrotracker.ui.adapters.DayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.macrotracker.R;
import com.example.macrotracker.ui.adapters.MonthAdapter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class CalendarStripView extends LinearLayout {

    public interface OnDaySelectedListener {
        void onDaySelected(LocalDate date);
    }

    private static final int DAY_BATCH = 14;
    private static final int MONTH_BATCH = 6;
    private static final int EDGE_THRESHOLD = 5;

    private RecyclerView monthRecycler, dayRecycler;
    private LinearLayoutManager dayLayoutManager, monthLayoutManager;
    private DayAdapter dayAdapter;
    private MonthAdapter monthAdapter;
    private OnDaySelectedListener listener;

    private boolean isLoadingDaysBefore = false;
    private boolean isLoadingDaysAfter = false;
    private boolean isLoadingMonthsBefore = false;
    private boolean isLoadingMonthsAfter = false;

    private YearMonth currentVisibleMonth = null;

    public CalendarStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_bordered_container_subtle);
        LayoutInflater.from(context).inflate(R.layout.calendar_strip, this, true);

        monthRecycler = findViewById(R.id.monthRecycler);
        dayRecycler = findViewById(R.id.dayRecycler);

        monthLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        dayLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        monthRecycler.setLayoutManager(monthLayoutManager);
        dayRecycler.setLayoutManager(dayLayoutManager);

        int daySpacing = dpToPx(8);
        dayRecycler.addItemDecoration(new HorizontalSpacingDecoration(daySpacing));

        int monthSpacing = dpToPx(16);
        monthRecycler.addItemDecoration(new HorizontalSpacingDecoration(monthSpacing));

        ImageButton prev = findViewById(R.id.monthPrevBtn);
        ImageButton next = findViewById(R.id.monthNextBtn);
        prev.setOnClickListener(v -> shiftMonth(-1));
        next.setOnClickListener(v -> shiftMonth(1));

        dayRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                checkDayEdges();
                syncMonthToVisibleDay();
            }
        });

        monthRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                checkMonthEdges();
            }
        });
    }

    public void setDays(List<LocalDate> days, LocalDate selected) {
        dayAdapter = new DayAdapter(days, selected, date -> {
            dayAdapter.setSelected(date);
            if (listener != null) listener.onDaySelected(date);
            syncMonthToVisibleDay();
        });
        dayRecycler.setAdapter(dayAdapter);
        scrollToDay(selected, false);
        currentVisibleMonth = YearMonth.from(selected);
    }

    public void setMonths(List<YearMonth> months, YearMonth selected) {
        monthAdapter = new MonthAdapter(months, selected, month -> {
            LocalDate resolvedDate = resolveDateForMonth(month);
            setSelectedDate(resolvedDate);
            if (listener != null) listener.onDaySelected(resolvedDate);
        });
        monthRecycler.setAdapter(monthAdapter);
        scrollToMonth(selected, false);
    }

    public void setOnDaySelectedListener(OnDaySelectedListener l) {
        this.listener = l;
    }

    // Called externally
    public void setSelectedDate(LocalDate date) {
        if (dayAdapter == null) return;

        int index = dayAdapter.indexOf(date);
        if (index == -1) {
            extendDayWindowToInclude(date);
        }

        dayAdapter.setSelected(date);
        scrollToDay(date, true);
        syncMonthToVisibleDay();
    }

    // lazy-loading day
    private void extendDayWindowToInclude(LocalDate date) {
        LocalDate firstLoaded = dayAdapter.getDayAt(0);
        LocalDate lastLoaded = dayAdapter.getDayAt(dayAdapter.getItemCount() - 1);

        if (date.isBefore(firstLoaded)) {
            dayAdapter.prependDays(buildDayRange(date, firstLoaded.minusDays(1)));
        } else if (date.isAfter(lastLoaded)) {
            dayAdapter.appendDays(buildDayRange(lastLoaded.plusDays(1), date));
        }
    }

    private void scrollToDay(LocalDate date, boolean smooth) {
        int index = dayAdapter.indexOf(date);
        if (index == -1) return;
        if (smooth) {
            dayRecycler.smoothScrollToPosition(index);
        } else {
            dayLayoutManager.scrollToPositionWithOffset(index, 0);
        }
    }

    private LocalDate resolveDateForMonth(YearMonth month) {
        YearMonth currentMonth = YearMonth.now();
        return month.equals(currentMonth) ? LocalDate.now() : month.atDay(1);
    }

    private void checkDayEdges() {
        if (dayAdapter == null) return;

        int first = dayLayoutManager.findFirstVisibleItemPosition();
        int last = dayLayoutManager.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return;

        if (!isLoadingDaysAfter && last >= dayAdapter.getItemCount() - EDGE_THRESHOLD) {
            loadMoreDaysAfter();
        }
        if (!isLoadingDaysBefore && first <= EDGE_THRESHOLD) {
            loadMoreDaysBefore();
        }
    }

    private void loadMoreDaysAfter() {
        isLoadingDaysAfter = true;
        LocalDate lastLoaded = dayAdapter.getDayAt(dayAdapter.getItemCount() - 1);
        dayAdapter.appendDays(buildDayRange(lastLoaded.plusDays(1), lastLoaded.plusDays(DAY_BATCH)));
        isLoadingDaysAfter = false;
    }

    private void loadMoreDaysBefore() {
        isLoadingDaysBefore = true;

        int anchorPos = dayLayoutManager.findFirstVisibleItemPosition();
        View anchorView = dayLayoutManager.findViewByPosition(anchorPos);
        int anchorOffset = anchorView != null ? anchorView.getLeft() : 0;

        LocalDate firstLoaded = dayAdapter.getDayAt(0);
        List<LocalDate> batch = buildDayRange(firstLoaded.minusDays(DAY_BATCH), firstLoaded.minusDays(1));
        dayAdapter.prependDays(batch);

        dayLayoutManager.scrollToPositionWithOffset(anchorPos + batch.size(), anchorOffset);
        isLoadingDaysBefore = false;
    }

    private List<LocalDate> buildDayRange(LocalDate start, LocalDate endInclusive) {
        List<LocalDate> range = new ArrayList<>();
        LocalDate d = start;
        while (!d.isAfter(endInclusive)) {
            range.add(d);
            d = d.plusDays(1);
        }
        return range;
    }

    // lazy-loading month

    private void checkMonthEdges() {
        if (monthAdapter == null) return;

        int first = monthLayoutManager.findFirstVisibleItemPosition();
        int last = monthLayoutManager.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return;

        if (!isLoadingMonthsAfter && last >= monthAdapter.getItemCount() - EDGE_THRESHOLD) {
            loadMoreMonthsAfter();
        }
        if (!isLoadingMonthsBefore && first <= EDGE_THRESHOLD) {
            loadMoreMonthsBefore();
        }
    }

    private void loadMoreMonthsAfter() {
        isLoadingMonthsAfter = true;
        YearMonth lastLoaded = monthAdapter.getMonthAt(monthAdapter.getItemCount() - 1);
        monthAdapter.appendMonths(buildMonthRange(lastLoaded.plusMonths(1), MONTH_BATCH));
        isLoadingMonthsAfter = false;
    }

    private void loadMoreMonthsBefore() {
        isLoadingMonthsBefore = true;

        int anchorPos = monthLayoutManager.findFirstVisibleItemPosition();
        View anchorView = monthLayoutManager.findViewByPosition(anchorPos);
        int anchorOffset = anchorView != null ? anchorView.getLeft() : 0;

        YearMonth firstLoaded = monthAdapter.getMonthAt(0);
        List<YearMonth> batch = buildMonthRange(firstLoaded.minusMonths(MONTH_BATCH), MONTH_BATCH);
        monthAdapter.prependMonths(batch);

        monthLayoutManager.scrollToPositionWithOffset(anchorPos + batch.size(), anchorOffset);
        isLoadingMonthsBefore = false;
    }

    private void extendMonthWindowToInclude(YearMonth month) {
        if (monthAdapter.indexOf(month) != -1) return;

        YearMonth firstLoaded = monthAdapter.getMonthAt(0);
        YearMonth lastLoaded = monthAdapter.getMonthAt(monthAdapter.getItemCount() - 1);

        if (month.isBefore(firstLoaded)) {
            int gapMonths = (int) month.until(firstLoaded, java.time.temporal.ChronoUnit.MONTHS);
            monthAdapter.prependMonths(buildMonthRange(month, gapMonths));
        } else if (month.isAfter(lastLoaded)) {
            int gapMonths = (int) lastLoaded.until(month, java.time.temporal.ChronoUnit.MONTHS);
            monthAdapter.appendMonths(buildMonthRange(lastLoaded.plusMonths(1), gapMonths));
        }
    }

    private List<YearMonth> buildMonthRange(YearMonth start, int count) {
        List<YearMonth> range = new ArrayList<>();
        YearMonth m = start;
        for (int i = 0; i < count; i++) {
            range.add(m);
            m = m.plusMonths(1);
        }
        return range;
    }

    private void scrollToMonth(YearMonth month, boolean smooth) {
        if (monthAdapter == null) return;
        int index = monthAdapter.indexOf(month);
        if (index == -1) return;
        if (smooth) {
            monthRecycler.smoothScrollToPosition(index);
        } else {
            monthLayoutManager.scrollToPositionWithOffset(index, 0);
        }
    }

    // Month day sync
    private void syncMonthToVisibleDay() {
        if (dayAdapter == null || monthAdapter == null) return;

        int first = dayLayoutManager.findFirstVisibleItemPosition();
        int last = dayLayoutManager.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return;

        int centerPos = (first + last) / 2;
        LocalDate centerDay = dayAdapter.getDayAt(centerPos);
        YearMonth visibleMonth = YearMonth.from(centerDay);

        if (visibleMonth.equals(currentVisibleMonth)) return;
        currentVisibleMonth = visibleMonth;

        extendMonthWindowToInclude(visibleMonth);
        monthAdapter.setSelected(visibleMonth);
        scrollToMonth(visibleMonth, true);
    }

    private void shiftMonth(int delta) {
        if (currentVisibleMonth == null) return;
        YearMonth target = currentVisibleMonth.plusMonths(delta);
        LocalDate resolvedDate = resolveDateForMonth(target);
        setSelectedDate(resolvedDate);
        if (listener != null) listener.onDaySelected(resolvedDate);
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
