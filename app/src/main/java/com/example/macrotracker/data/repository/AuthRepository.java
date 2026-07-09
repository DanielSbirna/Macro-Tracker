package com.example.macrotracker.data.repository;

import com.example.macrotracker.data.AuthCallback;
import com.example.macrotracker.data.remote.SupabaseAuthClient;

import org.json.JSONException;

import java.io.IOException;

public class AuthRepository {

    private final SupabaseAuthClient authClient;
    private final TokenStorage tokenStorage;

    public AuthRepository(SupabaseAuthClient authClient, TokenStorage tokenStorage) {
        this.authClient = authClient;
        this.tokenStorage = tokenStorage;
    }

    public void signUp(String email, String password, AuthCallback callback) {
        authClient.signUp(email, password, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                tokenStorage.saveTokens(accessToken, refreshToken);
                callback.onSuccess(accessToken, refreshToken);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        authClient.signIn(email, password, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                tokenStorage.saveTokens(accessToken, refreshToken);
                callback.onSuccess(accessToken, refreshToken);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void refreshSession(AuthCallback callback) {
        String refreshToken = tokenStorage.getRefreshToken();
        if (refreshToken == null) {
            callback.onError(new IllegalStateException("No refresh token available"));
            return;
        }

        authClient.refreshSession(refreshToken, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String newRefreshToken) {
                tokenStorage.saveTokens(accessToken, newRefreshToken);
                callback.onSuccess(accessToken, newRefreshToken);
            }

            @Override
            public void onError(Exception e) {
                // refresh failed — treat session as dead
                tokenStorage.clearTokens();
                callback.onError(e);
            }
        });
    }

    // sync refresh wrapper
    public String refreshSessionSync() throws IOException, JSONException {
        String refreshToken = tokenStorage.getRefreshToken();
        if (refreshToken == null) {
            throw new IllegalStateException("No refresh token available");
        }

        try {
            String[] tokens = authClient.refreshSessionSync(refreshToken);
            tokenStorage.saveTokens(tokens[0], tokens[1]);
            return tokens[0];
        } catch (IOException | JSONException e) {
            tokenStorage.clearTokens();
            throw e;
        }
    }

    public void signOut(AuthCallback callback) {
        String accessToken = tokenStorage.getAccessToken();
        tokenStorage.clearTokens();

        if (accessToken == null) {
            callback.onSuccess(null, null);
            return;
        }

        authClient.signOut(accessToken, callback);
    }

    public boolean isLoggedIn() {
        return tokenStorage.getAccessToken() != null;
    }

    public String getAccessToken() {
        return tokenStorage.getAccessToken();
    }
}
