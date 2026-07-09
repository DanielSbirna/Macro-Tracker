package com.example.macrotracker.data.remote;

import androidx.annotation.NonNull;

import com.example.macrotracker.data.repository.AuthRepository;
import com.example.macrotracker.data.TokenStorage;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final TokenStorage tokenStorage;
    private final AuthRepository authRepository;
    private final Object refreshLock = new Object();

    public AuthInterceptor(TokenStorage tokenStorage, AuthRepository authRepository) {
        this.tokenStorage = tokenStorage;
        this.authRepository = authRepository;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (tokenStorage.isDeletionPending()) {
            throw new IOException("Account deletion pending — request blocked");
        }

        Request original = chain.request();
        String accessToken = tokenStorage.getAccessToken();
        Response response = chain.proceed(attachToken(original, accessToken));

        if (response.code() != 401) {
            return response;
        }
        response.close();

        String refreshed = refreshBlocking(accessToken);
        if (refreshed == null) {
            return chain.proceed(attachToken(original, accessToken));
        }
        return chain.proceed(attachToken(original, refreshed));
    }

    private Request attachToken(Request request, String token) {
        if (token == null) return request;
        return request.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
    }

    private String refreshBlocking(String staleToken) {
        synchronized (refreshLock) {
            String current = tokenStorage.getAccessToken();
            if (current != null && !current.equals(staleToken)) {
                return current; // someone else already refreshed
            }
            try {
                return authRepository.refreshSessionSync();
            } catch (Exception e) {
                return null;
            }
        }
    }
}