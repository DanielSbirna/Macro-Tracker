package com.example.macrotracker.data;

import com.example.macrotracker.BuildConfig;
import com.example.macrotracker.models.WeightLog;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
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

public class WeightLogRepository {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient authedClient;
    private final TokenStorage tokenStorage;

    public WeightLogRepository(OkHttpClient authedClient, TokenStorage tokenStorage) {
        this.authedClient = authedClient;
        this.tokenStorage = tokenStorage;
    }

    public void insertWeight(WeightLog weightLog, RepoCallback<WeightLog> callback) {
        Request request;
        try {
            RequestBody body = RequestBody.create(weightLog.toJson().toString(), JSON);
            request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/weight_logs")
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
                if (response.code() == 409) {
                    callback.onError(new IOException("A weigh-in for this date already exists"));
                    return;
                }
                handleSingleWeightResponse(response, "Insert weight failed", callback);
            }
        });
    }

    public void updateWeight(WeightLog weightLog, RepoCallback<WeightLog> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;
        if (weightLog.getWeightId() == null) {
            callback.onError(new IllegalArgumentException("Cannot update a weight log without a weightId"));
            return;
        }

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/weight_logs")
                .newBuilder()
                .addQueryParameter("weight_id", "eq." + weightLog.getWeightId())
                .addQueryParameter("user_id", "eq." + userId)
                .build();

        Request request;
        try {
            RequestBody body = RequestBody.create(weightLog.toJson().toString(), JSON);
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
                handleSingleWeightResponse(response, "Update weight failed", callback);
            }
        });
    }

    public void deleteWeight(Long weightId, RepoCallback<Void> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/weight_logs")
                .newBuilder()
                .addQueryParameter("weight_id", "eq." + weightId)
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
                    callback.onError(new IOException("Delete weight failed: " + response.code()));
                    return;
                }
                callback.onSuccess(null);
            }
        });
    }

    // Full weigh-in history
    public void getWeightHistory(RepoCallback<List<WeightLog>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/weight_logs")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .addQueryParameter("order", "date_recorded.desc")
                .build();

        fetchWeightList(url, callback);
    }

    // most recent weigh-in only - TDEE recalculation
    public void getLatestWeight(RepoCallback<WeightLog> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/weight_logs")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .addQueryParameter("order", "date_recorded.desc")
                .addQueryParameter("limit", "1")
                .build();

        fetchWeightList(url, new RepoCallback<List<WeightLog>>() {
            @Override
            public void onSuccess(List<WeightLog> result) {
                callback.onSuccess(result.isEmpty() ? null : result.get(0));
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void fetchWeightList(HttpUrl url, RepoCallback<List<WeightLog>> callback) {
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
                    callback.onError(new IOException("Fetch weight history failed: " + response.code()));
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    callback.onError(new IOException("Empty response body"));
                    return;
                }
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    List<WeightLog> logs = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        logs.add(WeightLog.fromJson(arr.getJSONObject(i)));
                    }
                    callback.onSuccess(logs);
                } catch (JSONException parseError) {
                    callback.onError(parseError);
                }
            }
        });
    }

    private void handleSingleWeightResponse(Response response, String errorPrefix, RepoCallback<WeightLog> callback) throws IOException {
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
            callback.onSuccess(WeightLog.fromJson(arr.getJSONObject(0)));
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
