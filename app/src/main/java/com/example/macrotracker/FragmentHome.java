package com.example.macrotracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.macrotracker.databinding.FragmentHomeBinding;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class FragmentHome extends Fragment {

    private FragmentHomeBinding binding;

    public FragmentHome() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateTopCard();
        bindGoalChangeButton();
        setupCalendarStrip();
    }

    private void setupCalendarStrip() {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> months = buildMonthRange(currentMonth); // e.g. 2 before, 2 after
        binding.calendarStrip.setMonths(months, currentMonth);

        LocalDate today = LocalDate.now();
        List<LocalDate> days = buildWeekDays(today);
        binding.calendarStrip.setDays(days, today);

        binding.calendarStrip.setOnDaySelectedListener(date -> {

        });
    }

    private List<YearMonth> buildMonthRange(YearMonth anchor) {
        List<YearMonth> months = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            months.add(anchor.plusMonths(i));
        }
        return months;
    }

    private List<LocalDate> buildWeekDays(LocalDate anchor) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate start = anchor.minusDays(3); // 3 before + today + 3 after = 7 days
        for (int i = 0; i < 7; i++) {
            days.add(start.plusDays(i));
        }
        return days;
    }

    private void updateTopCard() {
        boolean accountComplete = false;

        if (!accountComplete) {
            binding.completeAccLayout.setVisibility(View.VISIBLE);
            binding.goalLayout.setVisibility(View.GONE);
        } else {
            binding.completeAccLayout.setVisibility(View.GONE);
            binding.goalLayout.setVisibility(View.VISIBLE);
        }
    }

    private void bindGoalChangeButton() {
        binding.goalChangeBtn.setOnClickListener(v -> {

        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // avoid leaking the view
    }
}