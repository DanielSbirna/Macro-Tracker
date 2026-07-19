package com.example.macrotracker.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
public class LinearProgressView extends View {

    private float progress = 0f;
    private float max = 100f;

    private final int trackColor = Color.parseColor("#EAE8E2");
    private final int baseColor = Color.parseColor("#0057FF");   // blue
    private final int overflowColor = Color.parseColor("#FF334B"); // red

    private Paint trackPaint;
    private Paint progressPaint;
    private RectF rect;
    private float cornerRadius;

    public LinearProgressView(Context context) {
        super(context);
        init();
    }

    public LinearProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(trackColor);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.FILL);

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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cornerRadius = h / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        // Background track always the full-width faint bar
        rect.set(0, 0, width, height);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackPaint);

        if (max <= 0f) return;

        float percent = (progress / max) * 100f;
        float clampedPercent = Math.min(percent, 100f);

        // Opacity scales with percent complete (0-100% => alpha 0-255),
        int alpha = (int) Math.min(Math.max(clampedPercent / 100f * 255, 0), 255);
        progressPaint.setColor(baseColor);
        progressPaint.setAlpha(alpha);

        float fillWidth = width * (clampedPercent / 100f);
        rect.set(0, 0, fillWidth, height);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, progressPaint);

        // Excess beyond 100% drawn as a second, overlapping red bar on top
        if (percent > 100f) {
            float excessPercent = Math.min(percent - 100f, 100f);
            float excessWidth = width * (excessPercent / 100f);

            progressPaint.setColor(overflowColor);
            progressPaint.setAlpha(255);
            rect.set(0, 0, excessWidth, height);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, progressPaint);
        }
    }
}