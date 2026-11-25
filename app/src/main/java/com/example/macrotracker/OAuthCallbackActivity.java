package com.example.macrotracker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.models.User;
import com.example.macrotracker.ui.login.LoginCallback;
import com.example.macrotracker.ui.login.LoginViewModel;

/**
 * UI-less landing point for macrotracker://auth-callback?code=...
 * Exchanges the code for a session, then routes to MainActivity (or back to login on failure).
 */
public class OAuthCallbackActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Rotation / recreation would replay the (already consumed) code
        if (savedInstanceState != null) {
            finish();
            return;
        }

        Uri data = getIntent().getData();
        String code = data != null ? data.getQueryParameter("code") : null;
        if (code == null) {
            fail(getString(R.string.oauth_failed));
            return;
        }

        ServiceLocator serviceLocator = ServiceLocator.getInstance(this);
        LoginViewModel loginViewModel = new LoginViewModel(
                serviceLocator.authRepository, serviceLocator.userProfilesRepository);

        loginViewModel.completeOAuth(code, new LoginCallback() {
            @Override
            public void onLoggedIn(User user) {
                // user == null -> MainActivity shows the "complete your account" dialog
                runOnUiThread(() -> {
                    Intent intent = new Intent(OAuthCallbackActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onBlocked(String reason) {
                runOnUiThread(() -> fail(reason));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> fail(getString(R.string.oauth_failed)));
            }
        });
    }

    private void fail(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
