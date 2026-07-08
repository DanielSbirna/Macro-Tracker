package com.example.macrotracker.util;

import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.models.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

public class TdeeCalculator {
    // Fat of 25% of total kcal intake assures a amount good for all goals with enough to not affect hormone health
    private static final BigDecimal FAT_PERCENT_OF_CALORIES = new BigDecimal("0.25");

    // Kcal amount per macro
    private static final BigDecimal CALORIES_PER_GRAM_PROTEIN = new BigDecimal("4");
    private static final BigDecimal CALORIES_PER_GRAM_CARB = new BigDecimal("4");
    private static final BigDecimal CALORIES_PER_GRAM_FAT = new BigDecimal("9");

   // Formula-predicted TDEE (Mifflin-St Jeor * activity multiplier)
   public static BigDecimal calculateFormulaTdee(User user, BigDecimal currentWeightKg) {
       if (currentWeightKg == null || currentWeightKg.compareTo(BigDecimal.ZERO) <= 0) {
           throw new IllegalArgumentException("currentWeightKg must be positive");
       }

       BigDecimal heightCm = user.getHeight();
       int age = user.getAge();
       char gender = user.getGender();

       // Mifflin-St Jeor BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age + s (where s male = +5, female = -161)
       BigDecimal bmr = currentWeightKg.multiply(new BigDecimal("10"))
               .add(heightCm.multiply(new BigDecimal("6.25")))
               .subtract(new BigDecimal(age).multiply(new BigDecimal("5")));
       bmr = bmr.add(gender == 'M' ? new BigDecimal("5") : new BigDecimal("-161"));

       // TDEE BMR * activityMultiplier
       return bmr.multiply(user.getActivityMultiplier());
   }

   // Build a full TargetMacros form given TDEE (formula-based or recalibrated)
   public static TargetMacros buildTargetFromTdee(User user, BigDecimal currentWeightKg, BigDecimal tdee) {
       BigDecimal calorieOffset = goalCalorieOffset(user.getCurrentGoal());
       BigDecimal targetCalories = tdee.add(calorieOffset);
       if (targetCalories.compareTo(BigDecimal.ZERO) <= 0) {
           throw new IllegalStateException("Calculated calorie target is negative; check inputs");
       }

       BigDecimal proteinPerKg = goalProteinPerKg(user.getCurrentGoal());
       BigDecimal proteinGrams = currentWeightKg.multiply(proteinPerKg);
       BigDecimal proteinCalories = proteinGrams.multiply(CALORIES_PER_GRAM_PROTEIN);

       BigDecimal fatCalories = targetCalories.multiply(FAT_PERCENT_OF_CALORIES);
       BigDecimal fatGrams = fatCalories.divide(CALORIES_PER_GRAM_FAT, 2, RoundingMode.HALF_UP);

       BigDecimal remainingCalories = targetCalories.subtract(proteinCalories).subtract(fatCalories);
       if (remainingCalories.compareTo(BigDecimal.ZERO) < 0) {
           throw new IllegalStateException("Protein and fat targets exceed total calorie target; check inputs");
       }
       BigDecimal carbGrams = remainingCalories.divide(CALORIES_PER_GRAM_CARB, 2, RoundingMode.HALF_UP);

       return new TargetMacros(
               user.getUserId(),
               OffsetDateTime.now(),
               targetCalories.setScale(2, RoundingMode.HALF_UP),
               proteinGrams.setScale(2, RoundingMode.HALF_UP),
               carbGrams,
               fatGrams
       );
   }

   // formula TDEE straight into a target
    public static TargetMacros calculate(User user, BigDecimal currentWeightKg) {
       BigDecimal tdee = calculateFormulaTdee(user, currentWeightKg);
       return buildTargetFromTdee(user, currentWeightKg, tdee);
    }

    // Standard slight cut deficit and bulk surplus
    private static BigDecimal goalCalorieOffset(String goal) {
        switch (goal) {
            case "cut": return new BigDecimal("-300");
            case "bulk": return new BigDecimal(300);
            case "main": return BigDecimal.ZERO;
            default: throw new IllegalArgumentException("Unknown goal: " + goal);
        }
    }

    // Standard protein intake per kg body weight
    private static BigDecimal goalProteinPerKg(String goal) {
        switch (goal) {
            case "cut": return new BigDecimal("2.0");
            case "bulk": return new BigDecimal("1.5");
            case "main": return new BigDecimal("1.7");
            default: throw new IllegalArgumentException("Unknown goal: " + goal);
        }
    }
}
