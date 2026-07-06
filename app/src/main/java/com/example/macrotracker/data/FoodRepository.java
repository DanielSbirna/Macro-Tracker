package com.example.macrotracker.data;

import com.example.macrotracker.BuildConfig;
import com.example.macrotracker.models.User;
import com.example.macrotracker.util.JwtUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.OffsetDateTime;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class FoodRepository {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient authedClient;
    private final TokenStorage tokenStorage;

    public FoodRepository(OkHttpClient authedClient, TokenStorage tokenStorage) {
        this.authedClient = authedClient;
        this.tokenStorage = tokenStorage;
    }

    public void getProfile(RepoCallback<User> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/user_profiles")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .addQueryParameter("select", "*")
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
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("Fetch profile failed: " + response.code()));
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
                        callback.onSuccess(null); // no row yet -> onboarding not complete
                        return;
                    }
                    callback.onSuccess(User.fromJson(arr.getJSONObject(0)));
                } catch (JSONException parseError) {
                    callback.onError(parseError);
                }
            }
        });
    }

    public void insertProfile(User user, RepoCallback<User> callback) {
        Request request;
        try {
            RequestBody body = RequestBody.create(user.toJson().toString(), JSON);
            request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/user_profiles")
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
                handleSingleRowResponse(response, "Insert profile failed", callback);
            }
        });
    }

    public void updateProfile(User user, RepoCallback<User> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/user_profiles")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .build();

        Request request;
        try {
            RequestBody body = RequestBody.create(user.toJson().toString(), JSON);
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
                handleSingleRowResponse(response, "Update profile failed", callback);
            }
        });
    }

    private void handleSingleRowResponse(Response response, String errorPrefix, RepoCallback<User> callback) throws IOException {
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
            callback.onSuccess(User.fromJson(arr.getJSONObject(0)));
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

    public void flagForDeletion(RepoCallback<Void> callback) {
        String userId = resolveUserIdOrFail(callback);
        if (userId == null) return;

        HttpUrl url = HttpUrl.parse(SUPABASE_URL + "/rest/v1/user_profiles")
                .newBuilder()
                .addQueryParameter("user_id", "eq." + userId)
                .build();

        JSONObject patchBody = new JSONObject();
        try {
            patchBody.put("deletion_requested_at", OffsetDateTime.now().toString());
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .patch(RequestBody.create(patchBody.toString(), JSON))
                .build();

        authedClient.newCall(request).enqueue(new Callback() {
           @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
               callback.onError(e);
           }

           @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
               if (!response.isSuccessful()) {
                   callback.onError(new IOException("Flag for deletion failed: " + response.code()));
                   return;
               }
               callback.onSuccess(null);
           }

        });
    }
}
