package com.example.macrotracker.data;

import com.example.macrotracker.GeminiApiClient;
import com.example.macrotracker.MacroCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public class SuggestionRepository {
    private final GeminiApiClient geminiApiClient;

    public SuggestionRepository(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
    }

    public void estimateMacros(String description, RepoCallback<MacroEstimate> callback ) {
        geminiApiClient.estimateMacros(description, new MacroCallback() {
            @Override
            public void onSuccess(String responseJson) {
                try {
                    JSONObject json = new JSONObject(responseJson);
                    MacroEstimate estimate = new MacroEstimate(
                            new BigDecimal(json.get("calories").toString()),
                            new BigDecimal(json.get("protein").toString()),
                            new BigDecimal(json.get("carbs").toString()),
                            new BigDecimal(json.get("fats").toString())
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
