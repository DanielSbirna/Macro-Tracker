package com.example.macrotracker.ui;

import android.content.Context;
import android.view.View;

import com.example.macrotracker.R;
import com.example.macrotracker.databinding.CardTrendBinding;
import com.example.macrotracker.models.StreakStatus;

/**
 * Handles rendering of the trend card (icon/title/body) based on a StreakStatus.
 * Kept fragment-local: instantiate in FragmentHome, call render() when data changes.
 */
public class TrendCardController {

    private final CardTrendBinding binding;
    private final Context context;

    public TrendCardController(CardTrendBinding binding, Context context) {
        this.binding = binding;
        this.context = context;
    }

    public void render(StreakStatus status, int streakDays) {
        binding.getRoot().setVisibility(View.VISIBLE);

        switch (status) {
            case CRUSHING_IT:
                binding.trendIcon.setImageResource(R.drawable.ic_trending_up_blue);
                binding.trendTitle.setText(R.string.trend_crushing_title);
                binding.trendBody.setText(context.getString(R.string.trend_crushing_body, streakDays));
                break;

            case STEADY:
                binding.trendIcon.setImageResource(R.drawable.ic_trending_flat_blue);
                binding.trendTitle.setText(R.string.trend_steady_title);
                binding.trendBody.setText(R.string.trend_steady_body);
                break;

            case NEEDS_ADJUSTMENT:
                binding.trendIcon.setImageResource(R.drawable.ic_trending_down_blue);
                binding.trendTitle.setText(R.string.trend_needs_adjustment_title);
                binding.trendBody.setText(R.string.trend_needs_adjustment_body);
                break;
        }
    }

    public void hide() {
        binding.getRoot().setVisibility(View.GONE);
    }
}