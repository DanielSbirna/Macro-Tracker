package com.example.macrotracker.data.repository;

import com.example.macrotracker.models.MacroEstimate;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.remote.GeminiApiClient;
import com.example.macrotracker.models.TargetMacros;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public class SuggestionRepository {
    private final GeminiApiClient geminiApiClient;

    public SuggestionRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void estimateMealWithSuggestion(String description, BigDecimal caloriesSoFar, BigDecimal proteinSoFar,
                                           BigDecimal carbsSoFar, BigDecimal fatsSoFar, TargetMacros target,
                                           RepoCallback<MacroEstimate> callback) {
        geminiApiClient.estimateMealWithSuggestion(description, caloriesSoFar, proteinSoFar, carbsSoFar, fatsSoFar, target,
                new RepoCallback<String>() {
                    @Override
                    public void onSuccess(String responseJson) {
                        try {
                            JSONObject json = new JSONObject(responseJson);
                            MacroEstimate estimate = new MacroEstimate(
                                    new BigDecimal(json.get("calories").toString()),
                                    new BigDecimal(json.get("protein").toString()),
                                    new BigDecimal(json.get("carbs").toString()),
                                    new BigDecimal(json.get("fats").toString()),
                                    json.getString("suggestion")
                            );
                            callback.onSuccess(estimate);
                        } catch (JSONException e) {
                            callback.onError(e);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
    }
}
