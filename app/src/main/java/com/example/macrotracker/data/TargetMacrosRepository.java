package com.example.macrotracker.data;

import com.example.macrotracker.BuildConfig;
import com.example.macrotracker.models.TargetMacros;
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
public class TargetMacrosRepository {
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient authedClient;
    private final TokenStorage tokenStorage;

    public TargetMacrosRepository(OkHttpClient authedClient, TokenStorage tokenStorage) {
        this.authedClient = authedClient;
        this.tokenStorage = tokenStorage;
    }

    public void insertTarget(TargetMacros target, RepoCallback<TargetMacros> callback) {
        Request request;
        try {
            RequestBody body = RequestBody.create(target.toJson().toString(), JSON);
            request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/target_macros")
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
                if(!response.isSuccessful()) {
                    callback.onError(new IOException("Insert target failed: " + response.code()));
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
                        callback.onError(new IOException("Insert target failed: no row returned"));
                        return;
                    }
                    callback.onSuccess(TargetMacros.fromJson(arr.getJSONObject(0)));
                } catch (JSONException parseError) {
                    callback.onError(parseError);
                }
            }
        });
    }

    // Full target history for the user, recent first
    public void getTargetHistory(RepoCallback<List<TargetMacros>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/target_macros")
                .newBuilder()
                .addQueryParameter("user_id","eq." + userId)
                .addQueryParameter("order", "created_at.desc")
                .build();

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
                if(!response.isSuccessful()) {
                    callback.onError(new IOException("Fetch target history failed " + response.code()));
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    callback.onError(new IOException("Empty response body"));
                    return;
                }
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    List<TargetMacros> targets = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        targets.add(TargetMacros.fromJson(arr.getJSONObject(i)));
                    }
                    callback.onSuccess(targets);
                } catch (JSONException parseError) {
                    callback.onError(parseError);
                }
            }
        });
    }

    // latest target only
    public void getLatestTarget(RepoCallback<TargetMacros> callback) {
        getTargetHistory(new RepoCallback<List<TargetMacros>>() {
            @Override
            public void onSuccess(List<TargetMacros> history) {
                callback.onSuccess(history.isEmpty() ? null : history.get(0));
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
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
