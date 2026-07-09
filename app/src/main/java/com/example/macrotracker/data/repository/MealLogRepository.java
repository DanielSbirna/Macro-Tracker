package com.example.macrotracker.data.repository;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.TokenStorage;
import com.example.macrotracker.data.remote.SupabaseRestClient;
import com.example.macrotracker.models.Meal;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MealLogRepository {

    private final SupabaseRestClient restClient;
    private final TokenStorage tokenStorage;

    public MealLogRepository(SupabaseRestClient restClient, TokenStorage tokenStorage) {
        this.restClient = restClient;
        this.tokenStorage = tokenStorage;
    }

    public void insertMeal(Meal meal, RepoCallback<Meal> callback) {
        String jsonBody;
        try {
            jsonBody = meal.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.insert("meals", jsonBody, new RepoCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    if (arr.length() == 0) {
                        callback.onError(new IllegalStateException("Insert meal failed: no row returned"));
                        return;
                    }
                    callback.onSuccess(Meal.fromJson(arr.getJSONObject(0)));
                } catch (JSONException parseError) {
                    callback.onError(parseError);
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
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

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"meal_id", "eq." + meal.getMealId()});
        params.add(new String[]{"user_id", "eq." + userId});

        String jsonBody;
        try{
            jsonBody = meal.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.update("meals", params, jsonBody, new RepoCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                handleSingleMealResponse(responseBody, "Update meal failed", callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void deleteMeal(Long mealId, RepoCallback<Void> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"meal_id", "eq." + mealId});
        params.add(new String[]{"user_id", "eq." + userId});

        restClient.delete("meals", params, new RepoCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // Last N meals, most recent first — for the dashboard.
    public void getRecentMeals(int limit, RepoCallback<List<Meal>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});
        params.add(new String[]{"order", "logged_at.desc"});
        params.add(new String[]{"limit", String.valueOf(limit)});

        fetchMealList(params, callback);
    }

    // All meals whose logged_at falls within [start, end) — for the calendar day view
    public void getMealsBetween(OffsetDateTime start, OffsetDateTime end, RepoCallback<List<Meal>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});
        params.add(new String[]{"logged_at", "gte." + start.toString()}); // greater or equal - gte
        params.add(new String[]{"logged_at", "lt." + end.toString()}); // less - lt.
        params.add(new String[]{"order", "logged_at.asc"});

        fetchMealList(params, callback);
    }

    private void fetchMealList(List<String[]> params, RepoCallback<List<Meal>> callback) {
        restClient.select("meals", params, new RepoCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
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

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void handleSingleMealResponse(String responseBody, String errorPrefix, RepoCallback<Meal> callback) {
        try {
            JSONArray arr = new JSONArray(responseBody);
            if (arr.length() == 0) {
                callback.onError(new IllegalStateException(errorPrefix + ": no row returned"));
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