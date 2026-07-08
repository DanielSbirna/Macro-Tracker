package com.example.macrotracker.data;

import com.example.macrotracker.models.WeightLog;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class WeightLogRepository {

    private final SupabaseRestClient restClient;
    private final TokenStorage tokenStorage;

    public WeightLogRepository(SupabaseRestClient restClient, TokenStorage tokenStorage) {
        this.restClient = restClient;
        this.tokenStorage = tokenStorage;
    }

    public void insertWeight(WeightLog weightLog, RepoCallback<WeightLog> callback) {
        String jsonBody;
        try {
            jsonBody = weightLog.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.insert("weight_logs", jsonBody, new SupabaseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                handleSingleWeightResponse(responseBody, "Insert weight Failed", callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
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

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"weight_id", "eq." + weightLog.getWeightId()});
        params.add(new String[]{"useri_id", "eq." + userId});

        String jsonBody;
        try {
            jsonBody = weightLog.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.update("weight_logs", params, jsonBody, new SupabaseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                handleSingleWeightResponse(responseBody, "Update weight failed", callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void deleteWeight(Long weightId, RepoCallback<Void> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"weight_id", "eq." + weightId});
        params.add(new String[]{"user_id", "eq." + userId});

        restClient.delete("weight_logs", params, new SupabaseCallback() {
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

    // Full weigh-in history
    public void getWeightHistory(RepoCallback<List<WeightLog>> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});
        params.add(new String[]{"order", "date_recorded.desc"});

        fetchWeightList(params, callback);
    }

    // most recent weigh-in only - TDEE recalculation
    public void getLatestWeight(RepoCallback<WeightLog> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});
        params.add(new String[]{"order", "date_recorded.desc"});
        params.add(new String[]{"limit", "1"});

        fetchWeightList(params, new RepoCallback<List<WeightLog>>() {
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


    private void fetchWeightList(List<String[]> params, RepoCallback<List<WeightLog>> callback) {
        restClient.select("weight_logs", params, new SupabaseCallback() {
            @Override
            public void onSuccess(String responseBody) {
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

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void handleSingleWeightResponse(String responseBody, String errorPrefix, RepoCallback<WeightLog> callback) {
        try {
            JSONArray arr = new JSONArray(responseBody);
            if (arr.length() == 0) {
                callback.onError(new IllegalStateException(errorPrefix + ": no row returned"));
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
