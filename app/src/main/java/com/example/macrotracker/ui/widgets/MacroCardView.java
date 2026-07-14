package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.macrotracker.R;

public class MacroCardView extends LinearLayout {

    private TextView labelView, valueView;
    private CircularProgressView ring;

    public MacroCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        int padTop = (int) (20 * getResources().getDisplayMetrics().density);
        setPadding(0, padTop, 0, 0);
        setBackgroundResource(R.drawable.bg_bordered_container_subtle);

        LayoutInflater.from(context).inflate(R.layout.macro_card, this, true);
        ring = findViewById(R.id.macroRing);
        labelView = findViewById(R.id.macroLabel);
        valueView = findViewById(R.id.macroValue);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MacroCardView);
        String label = a.getString(R.styleable.MacroCardView_macroLabel);
        if (label != null) labelView.setText(label);
        a.recycle();
    }

    public void setValue(String value) {
        valueView.setText(value);
    }

    public void setProgress(float progress) {
        ring.setProgress(progress); // adjust to your CircularProgressView's real API
    }
}
