package com.example.macrotracker.data.repository;

import com.example.macrotracker.data.AuthCallback;
import com.example.macrotracker.data.TokenStorage;
import com.example.macrotracker.data.remote.SupabaseAuthClient;
import com.example.macrotracker.util.PkceUtils;

import org.json.JSONException;

import java.io.IOException;

public class AuthRepository {

    // Social Login
    public static final String OAUTH_REDIRECT_URI = "macrotracker://auth-callback";

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

    // Social Login
    public String startOAuth(String provider) {
        String verifier = PkceUtils.generateCodeVerifier();
        tokenStorage.savePkceVerifier(verifier);
        return authClient.buildOAuthUrl(provider, OAUTH_REDIRECT_URI, PkceUtils.codeChallengeFor(verifier));
    }

    public void completeOAuth(String authCode, AuthCallback callback) {
        String verifier = tokenStorage.consumePkceVerifier();
        if (verifier == null) {
            callback.onError(new IllegalStateException("No pending OAuth request"));
            return;
        }
        authClient.exchangeAuthCode(authCode, verifier, new AuthCallback() {
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
