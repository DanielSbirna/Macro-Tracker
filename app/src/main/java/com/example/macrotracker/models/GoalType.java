package com.example.macrotracker.models;

import androidx.annotation.StringRes;

import com.example.macrotracker.R;

public enum GoalType {
    MAIN(R.string.goal_main),
    BULK(R.string.goal_bulk),
    CUT(R.string.goal_cut);

    @StringRes
    public final int label;

    GoalType(@StringRes int label) {
        this.label = label;
    }
}
