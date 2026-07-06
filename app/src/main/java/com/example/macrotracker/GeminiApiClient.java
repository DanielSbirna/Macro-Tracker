package com.example.macrotracker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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


    public void estimateMacros(String description, MacroCallback callback) {
        try {
            String prompt = "Estimate calories, protein, carbs and fats for this food description,"
                    + "assuming a typical serving if unspecified: \""
                    + description + "\". "
                    + "Respond ONLY with a JSON object in this exact format, no other text: "
                    + "{\"calories\": number, \"protein\": number, \"carbs\": number, \"fats\": number}";

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
                        callback.onError(new IOException("Request failed: " + response.code()));
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
