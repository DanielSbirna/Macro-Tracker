package com.example.macrotracker.data;

import com.example.macrotracker.BuildConfig;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MealLogRepository {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient authedClient;
    private final TokenStorage tokenStorage;

    public MealLogRepository(OkHttpClient authedClient, TokenStorage tokenStorage) {
        this.authedClient = authedClient;
        this.tokenStorage = tokenStorage;
    }

    public void insertMeal(Meal meal, RepoCallback<Meal> callback) {
        Request request;
        try {
            RequestBody body = RequestBody.create(meal.toJson().toString(), JSON);
            request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/meals")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        authedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleSingleMealResponse(response, "Insert meal failed", callback);
            }
        });
    }

    public void updateMeal(Meal meal, RepoCallback<Meal> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;
        if (meal.getMealId() == null) {
            callback.onError(new IllegalArgumentException("Cannot update a meal without a mealId"));
            return;
        }

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/meals")
                .newBuilder()
                .addQueryParameter("meal_id", "eq." + meal.getMealId())
                .addQueryParameter("user_id", "eq." + userId)
                .build();

        Request request;
        try {
            RequestBody body = RequestBody.create(meal.toJson().toString(), JSON);
            request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Prefer", "return=representation")
                    .patch(body)
                    .build();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        authedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleSingleMealResponse(response, "Update meal failed", callback);
            }
        });
    }

    public void deleteMeal(Long mealId, RepoCallback<Void> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/meals")
                .newBuilder()
                .addQueryParameter("meal_id", "eq." + mealId)
                .addQueryParameter("user_id", "eq." + userId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .delete()
                .build();

        authedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("Delete meal failed: " + response.code()));
                    return;
                }
                callback.onSuccess(null);
            }
        });
    }

    // Last N meals, most recent first — for the dashboard.
    public void getRecentMeals(int limit, RepoCallback<List<Meal>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/meals")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .addQueryParameter("order", "logged_at.desc")
                .addQueryParameter("limit", String.valueOf(limit))
                .build();

        fetchMealList(url, callback);
    }

    // All meals whose logged_at falls within [start, end) — for the calendar day view
    public void getMealsBetween(OffsetDateTime start, OffsetDateTime end, RepoCallback<List<Meal>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/meals")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .addQueryParameter("logged_at", "gte." + start.toString())
                .addQueryParameter("logged_at", "lt." + end.toString())
                .addQueryParameter("order", "logged_at.asc")
                .build();

        fetchMealList(url, callback);
    }

    private void fetchMealList(HttpUrl url, RepoCallback<List<Meal>> callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        authedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("Fetch meals failed: " + response.code()));
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    callback.onError(new IOException("Empty response body"));
                    return;
                }
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    List<Meal> meals = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        meals.add(Meal.fromJson(arr.getJSONObject(i)));
                    }
                    callback.onSuccess(meals);
                } catch (JSONException parseError) {
                    callback.onError(parseError);
                }
            }
        });
    }

    private void handleSingleMealResponse(Response response, String errorPrefix, RepoCallback<Meal> callback) throws IOException {
        if (!response.isSuccessful()) {
            callback.onError(new IOException(errorPrefix + ": " + response.code()));
            return;
        }
        String body = response.body() != null ? response.body().string() : null;
        if (body == null) {
            callback.onError(new IOException("Empty response body"));
            return;
        }
        try {
            JSONArray arr = new JSONArray(body);
            if (arr.length() == 0) {
                callback.onError(new IOException(errorPrefix + ": no row returned"));
                return;
            }
            callback.onSuccess(Meal.fromJson(arr.getJSONObject(0)));
        } catch (JSONException parseError) {
            callback.onError(parseError);
        }
    }

    private <T> String resolveUserIdOrFail(RepoCallback<T> callback) {
        String accessToken = tokenStorage.getAccessToken();
        if (accessToken == null) {
            callback.onError(new IllegalStateException("Not logged in"));
            return null;
        }
        try {
            return JwtUtils.getUserIdFromToken(accessToken);
        } catch (Exception e) {
            callback.onError(e);
            return null;
        }
    }
}