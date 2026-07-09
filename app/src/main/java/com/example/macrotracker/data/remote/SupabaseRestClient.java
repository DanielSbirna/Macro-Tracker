package com.example.macrotracker.data.remote;

import com.example.macrotracker.BuildConfig;
import com.example.macrotracker.data.RepoCallback;

import java.io.IOException;
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

public class SupabaseRestClient {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient authedClient;

    public SupabaseRestClient(OkHttpClient authedClient) {
        this.authedClient = authedClient;
    }

    // query parameters: list of {key, value} pairs - {"user_id", "eq.123"}
    public void select(String table, List<String[]> queryParams, RepoCallback<String> callback) {
        HttpUrl url = buildUrl(table, queryParams);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build();

        execute(request, "Select failed", callback);
    }

    public void insert(String table, String jsonBody, RepoCallback<String> callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/" + table)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Prefer", "return=representation")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        execute(request, "Insert failed", callback);
    }

    public void update(String table, List<String[]> queryParams, String jsonBody, RepoCallback<String> callback) {
        HttpUrl url = buildUrl(table, queryParams);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Prefer", "return=representation")
                .patch(RequestBody.create(jsonBody, JSON))
                .build();

        execute(request, "Update failed", callback);
    }

    public void delete(String table, List<String[]> queryParams, RepoCallback<String> callback) {
        HttpUrl url = buildUrl(table, queryParams);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .delete()
                .build();

        execute(request, "Delete failed", callback);
    }

    private HttpUrl buildUrl(String table, List<String[]> queryParams) {
        HttpUrl.Builder builder = HttpUrl.parse(SUPABASE_URL + "/rest/v1/" + table).newBuilder();
        if (queryParams != null) {
            for (String[] pair : queryParams) {
                builder.addQueryParameter(pair[0], pair[1]);
            }
        }
        return builder.build();
    }

    private void execute(Request request, String errorPrefix, RepoCallback<String> callback) {
        authedClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException(errorPrefix + ": " + response.code()));
                    return;
                }
                String body = response.body() != null ? response.body().string() : null;
                if (body == null) {
                    callback.onError(new IOException("Empty response body"));
                    return;
                }
                callback.onSuccess(body);
            }
        });
    }
}