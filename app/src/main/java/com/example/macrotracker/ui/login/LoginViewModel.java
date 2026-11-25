package com.example.macrotracker.ui.login;

import com.example.macrotracker.data.AuthCallback;
import com.example.macrotracker.data.repository.AuthRepository;
import com.example.macrotracker.data.repository.UserProfilesRepository;
import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.models.User;

public class LoginViewModel {
    private final AuthRepository authRepository;
    private final UserProfilesRepository userProfilesRepository;

    public LoginViewModel(AuthRepository authRepository, UserProfilesRepository userProfilesRepository) {
        this.authRepository = authRepository;
        this.userProfilesRepository = userProfilesRepository;
    }

    // Social Login
    public void login(String email, String password, LoginCallback callback) {
        authRepository.signIn(email, password, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                checkProfile(callback); // CHANGED (fix #1 social login): profile check moved into checkProfile()
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void completeOAuth(String authCode, LoginCallback callback) {
        authRepository.completeOAuth(authCode, new AuthCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken) {
                checkProfile(callback);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void checkProfile(LoginCallback callback) {
        userProfilesRepository.getProfile(new RepoCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.isFlaggedForDeletion()) {
                    authRepository.signOut(new AuthCallback() {
                        @Override
                        public void onSuccess(String a, String r) {
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                    callback.onBlocked("Account scheduled for deletion");
                    return;
                }
                callback.onLoggedIn(user); // null = onboarding not complete
            }

            @Override
            public void onError(Exception e) {
                callback.onLoggedIn(null);
            }
        });
    }
}

