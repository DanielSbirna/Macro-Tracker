package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {

    private float progress = 0f;   // current amount, e.g. 1500
    private float max = 100f;      // target amount, e.g. 3000

    private final float strokeWidth = 18f;
    private final int trackColor = Color.parseColor("#E0E0E0");
    private final int baseColor = Color.parseColor("#4285F4");   // blue
    private final int overflowColor = Color.parseColor("#E53935"); // red

    private Paint trackPaint;
    private Paint progressPaint;
    private RectF rect;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(trackColor);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        rect = new RectF();
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public void setMax(float max) {
        this.max = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float inset = strokeWidth / 2f;
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        // Background track — always a full faint circle
        canvas.drawArc(rect, 0f, 360f, false, trackPaint);

        if (max <= 0f) return;

        float percent = (progress / max) * 100f;
        float clampedPercent = Math.min(percent, 100f);
        float sweepAngle = (clampedPercent / 100f) * 360f;

        // Opacity scales with percent complete (0-100% => alpha 0-255)
        int alpha = (int) Math.min(Math.max(clampedPercent / 100f * 255, 0), 255);
        progressPaint.setColor(baseColor);
        progressPaint.setAlpha(alpha);

        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint);

        // Excess beyond 100% — drawn as a second, overlapping red arc, full opacity
        if (percent > 100f) {
            float excessPercent = Math.min(percent - 100f, 100f); // caps a 2nd full lap
            float excessSweep = (excessPercent / 100f) * 360f;

            progressPaint.setColor(overflowColor);
            progressPaint.setAlpha(255);
            canvas.drawArc(rect, -90f, excessSweep, false, progressPaint);
        }
    }
}