package com.example.macrotracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.macrotracker.data.ServiceLocator;
import com.example.macrotracker.databinding.ActivityLoginBinding;
import com.example.macrotracker.models.User;
import com.example.macrotracker.ui.login.LoginCallback;
import com.example.macrotracker.ui.login.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ServiceLocator serviceLocator = ServiceLocator.getInstance(this);
        loginViewModel = new LoginViewModel(serviceLocator.authRepository, serviceLocator.userProfilesRepository);

        binding.loginBtn.setOnClickListener(v -> attemptLogin());
        binding.signUpLink.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        // Social login (Google/Facebook) and "forgot password" aren't backed by any
        // repo call yet - leaving them inert rather than faking a flow.
    }

    private void attemptLogin() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();

        boolean valid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.emailError.setVisibility(View.INVISIBLE);
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            binding.passwordError.setVisibility(View.INVISIBLE);
        }

        if (!valid) return;

        setLoading(true);
        loginViewModel.login(email, password, new LoginCallback() {
            @Override
            public void onLoggedIn(User user) {
                // user == null just means onboarding/profile isn't complete yet -
                // FragmentHome already handles that state, so route home either way.
                runOnUiThread(() -> {
                    setLoading(false);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onBlocked(String reason) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, reason, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loginBtn.setEnabled(!loading);
    }
}