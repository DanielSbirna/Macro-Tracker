package com.example.macrotracker;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuthClient {

    //reusable OkHttpClient
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30,TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;

    public void signUp(String email, String password, AuthCallback callback) {
        try {
            Log.d("AuthTest", "Inside signUp try block");
            JSONObject requestJson = new JSONObject()
                    .put("email", email)
                    .put("password", password);

            RequestBody body = RequestBody.create(requestJson.toString(), JSON);

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/signup")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .post(body)
                    .build();

            Log.d("AuthTest", "Request built, URL: " + request.url());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(!response.isSuccessful()) {
                        callback.onError(new IOException("Signup failed: " + response.code()));
                        return;
                    }
                    String responseBody = response.body() !=null ? response.body().string() : null;
                    if(responseBody == null) {
                        callback.onError(new IOException("Empty response body"));
                        return;
                    }

                    // parsing could fail independently
                    try{
                        JSONObject json = new JSONObject(responseBody);
                        String accessToken = json.getString("access_token");
                        callback.onSuccess(accessToken);
                    } catch (Exception parseError) {
                        callback.onError(parseError);
                    }
                }
            });

            Log.d("AuthTest", "enqueue() called");

        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
