package com.example.macrotracker.data.repository;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.TokenStorage;
import com.example.macrotracker.data.remote.SupabaseRestClient;
import com.example.macrotracker.models.TargetMacros;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class TargetMacrosRepository {
    private final SupabaseRestClient restClient;
    private final TokenStorage tokenStorage;

    public TargetMacrosRepository(SupabaseRestClient restClient, TokenStorage tokenStorage) {
        this.restClient = restClient;
        this.tokenStorage = tokenStorage;
    }

    public void insertTarget(TargetMacros target, RepoCallback<TargetMacros> callback) {
        String jsonBody;
        try {
            jsonBody = target.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.insert("target_macros", jsonBody, new RepoCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    if (arr.length() == 0) {
                        callback.onError(new IllegalStateException("Insert target failed: no row returned"));
                        return;
                    }
                    callback.onSuccess(TargetMacros.fromJson(arr.getJSONObject(0)));
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

    // Full target history for the user, recent first
    public void getTargetHistory(RepoCallback<List<TargetMacros>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});
        params.add(new String[]{"order", "created_at.desc"});

        restClient.select("target_macros", params, new RepoCallback<String>() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    List<TargetMacros> targets = new ArrayList<>();
                    for (int i = 0; i< arr.length(); i++) {
                        targets.add(TargetMacros.fromJson(arr.getJSONObject(i)));
                    }
                    callback.onSuccess(targets);
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
