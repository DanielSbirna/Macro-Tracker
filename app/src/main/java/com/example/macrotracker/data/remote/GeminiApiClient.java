package com.example.macrotracker;

import com.example.macrotracker.models.TargetMacros;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class GeminiApiClient {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    public void estimateMealWithSuggestion(String description, BigDecimal caloriesSoFar,
                                           BigDecimal proteinSoFar, BigDecimal carbsSoFar, BigDecimal fatsSoFar,
                                           TargetMacros target, MacroCallback callback) {
        try {
            String prompt = "Estimate calories, protein, carbs and fats for this food description, "
                    + "assuming a typical serving if unspecified: \"" + description + "\". "
                    + "The user's daily targets are: calories " + target.getCalories()
                    + ", protein " + target.getProtein() + "g, carbs " + target.getCarbs()
                    + "g, fats " + target.getFats() + "g. "
                    + "So far today, before this meal, they've had: calories " + caloriesSoFar
                    + ", protein " + proteinSoFar + "g, carbs " + carbsSoFar + "g, fats " + fatsSoFar + "g. "
                    + "Respond ONLY with a JSON object in this exact format, no other text: "
                    + "{\"calories\": number, \"protein\": number, \"carbs\": number, \"fats\": number, "
                    + "\"suggestion\": \"one short sentence on what to add or drop from this meal so it fits better in the goal\"}";

            JSONObject part = new JSONObject().put("text", prompt);
            JSONObject content = new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(part));

            JSONObject requestJson = new JSONObject().put("contents", new JSONArray().put(content));

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);

            Request request = new Request.Builder()
                    .url(GEMINI_URL)
                    .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "no body";
                        callback.onError(new IOException("Request failed: " + response.code() + " - " + errorBody));
                        return;
                    }
                    String responseBody = response.body() != null ? response.body().string() : null;
                    if (responseBody == null) {
                        callback.onError(new IOException("Empty response body"));
                        return;
                    }
                    try {
                        JSONObject envelope = new JSONObject(responseBody);
                        String text = envelope
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        callback.onSuccess(text);
                    } catch (Exception parseError) {
                        callback.onError(parseError);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
