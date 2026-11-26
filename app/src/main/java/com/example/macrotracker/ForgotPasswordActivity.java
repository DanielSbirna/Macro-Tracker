package com.example.macrotracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String prefill = getIntent().getStringExtra(EXTRA_EMAIL);
        if (!TextUtils.isEmpty(prefill)) {
            binding.emailInput.setText(prefill);
        }

        binding.sendBtn.setOnClickListener(v -> attemptSend());
        binding.backToLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptSend() {
        String email = binding.emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailError.setVisibility(View.VISIBLE);
            return;
        }
        binding.emailError.setVisibility(View.INVISIBLE);

        setLoading(true);
        ServiceLocator.getInstance(this).authRepository.requestPasswordReset(email, new RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Same message whether or not the email has an account, so this screen
                // can't be used to find out who is registered.
                runOnUiThread(() -> {
                    setLoading(false);
                    binding.sentMessage.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    boolean rateLimited = e.getMessage() != null && e.getMessage().contains("429");
                    Toast.makeText(ForgotPasswordActivity.this,
                            rateLimited ? R.string.reset_rate_limited : R.string.reset_send_failed,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.sendBtn.setEnabled(!loading);
    }
}
