package com.example.macrotracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.macrotracker.data.AuthCallback;
import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.data.repository.AuthRepository;
import com.example.macrotracker.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = ServiceLocator.getInstance(this).authRepository;

        binding.backBtn.setOnClickListener(v -> finish());
        binding.signUpbtn.setOnClickListener(v -> attemptSignUp());
        binding.loginLink.setOnClickListener(v -> finish()); // back to LoginActivity

        // Social sign-up (Google/Facebook) isn't backed by any repo call yet.
    }

    private void attemptSignUp() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();
        String confirmPassword = binding.confirmPasswordInput.getText().toString();

        boolean valid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.emailError.setVisibility(View.INVISIBLE);
        }

        if (TextUtils.isEmpty(password) || password.length() < 8) {
            binding.passwordError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.passwordError.setVisibility(View.INVISIBLE);
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.confirmPasswordError.setVisibility(View.INVISIBLE);
        }

        if (!valid) return;

        setLoading(true);
        authRepository.signUp(email, password, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                // FragmentHome's "complete your account"
                runOnUiThread(() -> {
                    setLoading(false);
                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(SignUpActivity.this, R.string.sign_up_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.signUpbtn.setEnabled(!loading);
    }
}