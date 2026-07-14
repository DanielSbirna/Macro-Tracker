package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.macrotracker.R;

public class StatColumnView extends LinearLayout {

    private TextView labelView, valueView, percentView;

    public StatColumnView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_bordered_container_subtle);
        setGravity(Gravity.CENTER_HORIZONTAL);

        LayoutInflater.from(context).inflate(R.layout.stat_column, this, true);
        labelView = findViewById(R.id.statLabel);
        valueView = findViewById(R.id.statValue);
        percentView = findViewById(R.id.statPercent);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StatColumnView);
        String label = a.getString(R.styleable.StatColumnView_statLabel);
        if (label != null) labelView.setText(label);
        a.recycle();
    }

    public void setValue(String value) { valueView.setText(value); }
    public void setPercent(String percent) { percentView.setText(percent); }
}
