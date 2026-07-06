package com.example.macrotracker.data;

import com.example.macrotracker.SupabaseAuthClient;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final TokenStorage tokenStorage;
    private final SupabaseAuthClient authClient;

    public AuthInterceptor(TokenStorage tokenStorage, SupabaseAuthClient authClient) {
        this.tokenStorage = tokenStorage;
        this.authClient = authClient;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        String accessToken = tokenStorage.getAccessToken();
        Request requestWithAuth = attachToken(original, accessToken);

        Response response = chain.proceed(requestWithAuth);

        if (response.code() == 401) {
            response.close();

            String refreshToken = tokenStorage.getRefreshToken();
            if (refreshToken == null) {
                tokenStorage.clearTokens();
                return response; // caller sees the 401, nothing left to retry with
            }

            try {
                String[] newTokens = authClient.refreshSessionSync(refreshToken);
                tokenStorage.saveTokens(newTokens[0], newTokens[1]);

                Request retryRequest = attachToken(original, newTokens[0]);
                return chain.proceed(retryRequest);
            } catch (Exception refreshError) {
                tokenStorage.clearTokens();
                return response; // refresh itself failed, session is dead
            }
        }

        return response;
    }

    private Request attachToken(Request original, String accessToken) {
        if (accessToken == null) return original;
        return original.newBuilder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
    }
}