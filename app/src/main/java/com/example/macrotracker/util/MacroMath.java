package com.example.macrotracker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class MacroMath {

    public static float percentOf(BigDecimal eaten, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) return 0f;
        return eaten.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .floatValue();
    }

    public static String formatWhole(BigDecimal value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }

    public static String formatPercent(float percent) {
        if (Float.isNaN(percent) || Float.isInfinite(percent)) percent = 0f;
        return String.format(Locale.getDefault(), "%.1f", percent);
    }
}