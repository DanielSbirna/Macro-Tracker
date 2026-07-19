package com.example.macrotracker.models;

public interface FoodLogRow {
    int TYPE_HEADER = 0;
    int TYPE_MEAL = 1;
    int TYPE_EMPTY = 2;

    enum CardPosition { SINGLE, TOP, MIDDLE, BOTTOM }

    class HeaderRow implements FoodLogRow {
        public final MealType type;
        public HeaderRow(MealType type) { this.type = type; }
    }

    class MealRow implements FoodLogRow {
        public final Meal meal;
        public final CardPosition position;
        public MealRow(Meal meal, CardPosition position) {
            this.meal = meal;
            this.position = position;
        }
    }
}