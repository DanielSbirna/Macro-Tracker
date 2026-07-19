package com.example.macrotracker.models;

public enum MealType {
    BREAKFAST("Breakfast", "ic_breakfast_blue", "breakfast"),
    LUNCH("Lunch", "ic_lunch_blue", "lunch"),
    DINNER("Dinner", "ic_dinner_blue", "dinner"),
    SNACKS("Snacks", "ic_snacks_blue", "snack");

    private final String displayName;
    private final String iconName;
    private final String dbValue;

    MealType(String displayName, String iconName, String dbValue) {
        this.displayName = displayName;
        this.iconName = iconName;
        this.dbValue = dbValue;
    }

    public String getDisplayName() {return displayName;}
    public String getIconName() {return iconName;}
    public String getDbValue() {return dbValue;}

    public static MealType fromDbValue(String value) {
        for (MealType t : values()) {
            if (t.dbValue.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown meal type: " + value);
    }
}
