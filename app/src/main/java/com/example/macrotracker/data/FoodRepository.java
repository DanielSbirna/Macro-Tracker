package com.example.macrotracker.data;

import com.example.macrotracker.models.User;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class FoodRepository {

    private final SupabaseRestClient restClient;
    private final TokenStorage tokenStorage;

    public FoodRepository(SupabaseRestClient restClient, TokenStorage tokenStorage) {
        this.restClient = restClient;
        this.tokenStorage = tokenStorage;
    }

    public void getProfile(RepoCallback<User> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});
        params.add(new String[]{"select", "*"});

        restClient.select("user_profiles", params, new SupabaseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    if (arr.length() == 0) {
                        callback.onSuccess(null); // no row yet -> onboarding not complete
                        return;
                    }
                    callback.onSuccess(User.fromJson(arr.getJSONObject(0)));
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

    public void insertProfile(User user, RepoCallback<User> callback) {
        String jsonBody;
        try {
            jsonBody = user.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.insert("user_profiles", jsonBody, new SupabaseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                parseSingleUser(responseBody, "Insert profile failed", callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updateProfile(User user, RepoCallback<User> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});

        String jsonBody;
        try {
            jsonBody = user.toJson().toString();
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.update("user_profiles", params, jsonBody, new SupabaseCallback() {
            @Override
            public void onSuccess(String responseBody) {
                parseSingleUser(responseBody, "Update profile failed", callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void flagForDeletion(RepoCallback<Void> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        List<String[]> params = new ArrayList<>();
        params.add(new String[]{"user_id", "eq." + userId});

        JSONObject patchBody = new JSONObject();
        try {
            patchBody.put("deletion_requested_at", OffsetDateTime.now().toString());
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        restClient.update("user_profiles", params, patchBody.toString(), new SupabaseCallback() {
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

    private void parseSingleUser(String responseBody, String errorPrefix, RepoCallback<User> callback) {
        try {
            JSONArray arr = new JSONArray(responseBody);
            if (arr.length() == 0) {
                callback.onError(new IllegalStateException(errorPrefix + ": no row returned"));
                return;
            }
            callback.onSuccess(User.fromJson(arr.getJSONObject(0)));
        } catch (JSONException e) {
            callback.onError(e);
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