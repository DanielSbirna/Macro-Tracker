package com.example.macrotracker.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SlidingIndicatorView extends View {
    private Paint paint;
    private RectF rect;
    private float currentX = 0f;
    private float indicatorWidth;
    private float indicatorHeight;

    public SlidingIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#4285F4"));
        rect = new RectF();
    }

    public void setIndicatorSize(float width, float height) {
        this.indicatorWidth = width;
        this.indicatorHeight = height;
    }

    public void moveTo(float targetX) {
        ValueAnimator animator = ValueAnimator.ofFloat(currentX, targetX);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            currentX = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float top = (getHeight() - indicatorHeight) / 2f;
        rect.set(currentX, top, currentX + indicatorWidth, top + indicatorHeight);
        canvas.drawRoundRect(rect, 24f, 24f, paint);
    }
}