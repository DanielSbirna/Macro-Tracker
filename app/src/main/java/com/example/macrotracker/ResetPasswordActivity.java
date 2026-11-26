package com.example.macrotracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.macrotracker.data.AuthCallback;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.data.repository.AuthRepository;
import com.example.macrotracker.databinding.ActivityResetPasswordBinding;

/**
 * Landing point for macrotracker://reset-password?code=...
 * Exchanges the code for a short-lived recovery session, lets the user pick a new
 * password, then sends them to the login screen. The recovery tokens are kept in memory only.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 8; // same rule as SignUpActivity

    private ActivityResetPasswordBinding binding;
    private AuthRepository authRepository;
    private String recoveryAccessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = ServiceLocator.getInstance(this).authRepository;
        binding.updateBtn.setOnClickListener(v -> attemptUpdate());
        setFormEnabled(false);

        Uri data = getIntent().getData();
        String code = data != null ? data.getQueryParameter("code") : null;
        if (code == null) {
            failToLogin(R.string.reset_link_invalid);
            return;
        }

        authRepository.exchangeRecoveryCode(code, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                runOnUiThread(() -> {
                    recoveryAccessToken = accessToken;
                    binding.verifyingText.setVisibility(View.GONE);
                    setFormEnabled(true);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> failToLogin(R.string.reset_link_invalid));
            }
        });
    }

    private void attemptUpdate() {
        String password = binding.newPasswordInput.getText().toString();
        String confirm = binding.confirmPasswordInput.getText().toString();

        boolean valid = true;

        if (TextUtils.isEmpty(password) || password.length() < MIN_PASSWORD_LENGTH) {
            binding.newPasswordError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.newPasswordError.setVisibility(View.INVISIBLE);
        }

        if (!password.equals(confirm)) {
            binding.confirmPasswordError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.confirmPasswordError.setVisibility(View.INVISIBLE);
        }

        if (!valid || recoveryAccessToken == null) return;

        setFormEnabled(false);
        authRepository.updatePassword(recoveryAccessToken, password, new RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> failToLogin(R.string.password_updated));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    String message = e.getMessage() != null ? e.getMessage() : "";
                    if (message.contains("401")) {
                        failToLogin(R.string.reset_link_invalid); // recovery session expired
                        return;
                    }
                    setFormEnabled(true);
                    Toast.makeText(ResetPasswordActivity.this,
                            message.contains("422") ? R.string.reset_password_rejected : R.string.reset_update_failed,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setFormEnabled(boolean enabled) {
        binding.newPasswordInput.setEnabled(enabled);
        binding.confirmPasswordInput.setEnabled(enabled);
        binding.updateBtn.setEnabled(enabled);
    }

    // Used for success too: show a message, then land on the login screen
    private void failToLogin(int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
