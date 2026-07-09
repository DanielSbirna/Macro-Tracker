package com.example.macrotracker.logic;

import com.example.macrotracker.data.repository.MealLogRepository;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.repository.TargetMacrosRepository;
import com.example.macrotracker.data.repository.WeightLogRepository;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.models.User;
import com.example.macrotracker.models.WeightLog;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecalibrationChecker {

    private final TargetMacrosRepository targetMacrosRepository;
    private final MealLogRepository mealLogRepository;
    private final WeightLogRepository weightLogRepository;

    public RecalibrationChecker(TargetMacrosRepository targetMacrosRepository,
                                MealLogRepository mealLogRepository,
                                WeightLogRepository weightLogRepository) {
        this.targetMacrosRepository = targetMacrosRepository;
        this.mealLogRepository = mealLogRepository;
        this.weightLogRepository = weightLogRepository;
    }

    public void checkAndSuggest(User user, RecalibrationCallback callback) {
        targetMacrosRepository.getLatestTarget(new RepoCallback<TargetMacros>() {
            @Override
            public void onSuccess(TargetMacros latestTarget) {
                if (!RecalibrationEngine.isDueForRecalibration(latestTarget)) {
                    callback.onNoSuggestion("Not due for recalibration yet.");
                    return;
                }

                ZoneId zone = ZoneId.of(user.getTimezone());
                ZonedDateTime endZoned = ZonedDateTime.now(zone);
                ZonedDateTime startZoned = endZoned.minusDays(RecalibrationEngine.RECALIBRATION_WINDOW_DAYS);
                OffsetDateTime windowStart = startZoned.toOffsetDateTime();
                OffsetDateTime windowEnd = endZoned.toOffsetDateTime();

                mealLogRepository.getMealsBetween(windowStart, windowEnd, new RepoCallback<List<Meal>>() {
                    @Override
                    public void onSuccess(List<Meal> meals) {
                        weightLogRepository.getWeightHistory(new RepoCallback<List<WeightLog>>() {
                            @Override
                            public void onSuccess(List<WeightLog> fullHistory) {
                                List<WeightLog> inWindow = new ArrayList<>();
                                for (WeightLog log : fullHistory) {
                                    if (!log.getDateRecorded().isBefore(windowStart.toLocalDate())
                                            && !log.getDateRecorded().isAfter(windowEnd.toLocalDate())) {
                                        inWindow.add(log);
                                    }
                                }

                                RecalibrationResult result = RecalibrationEngine.evaluate(user, meals, inWindow);
                                if (result.getStatus() == RecalibrationResult.Status.SUCCESS) {
                                    callback.onSuggestionAvailable(result);
                                } else {
                                    callback.onNoSuggestion(result.getMessage());
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                callback.onError(e);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }
}