package com.example.macrotracker.data;

import android.content.Context;

import com.example.macrotracker.data.remote.AuthInterceptor;
import com.example.macrotracker.data.remote.GeminiApiClient;
import com.example.macrotracker.data.remote.SupabaseAuthClient;
import com.example.macrotracker.data.remote.SupabaseRestClient;
import com.example.macrotracker.data.repository.AuthRepository;
import com.example.macrotracker.data.repository.MealLogRepository;
import com.example.macrotracker.data.repository.SuggestionRepository;
import com.example.macrotracker.data.repository.TargetMacrosRepository;
import com.example.macrotracker.data.repository.UserProfilesRepository;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class ServiceLocator {

    private static volatile ServiceLocator instance;

    public final TokenStorage tokenStorage;
    public final SupabaseAuthClient authClient;
    public final AuthRepository authRepository;
    public final SupabaseRestClient restClient;
    public final GeminiApiClient geminiApiClient;
    public final SuggestionRepository suggestionRepository;
    public final MealLogRepository mealLogRepository;
    public final TargetMacrosRepository targetMacrosRepository;
    public final UserProfilesRepository userProfilesRepository;

    private ServiceLocator(Context appContext) {
        tokenStorage = new TokenStorageImpl(appContext);
        authClient = new SupabaseAuthClient();
        authRepository = new AuthRepository(authClient, tokenStorage);

        OkHttpClient authedHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(tokenStorage, authRepository))
                .build();

        restClient = new SupabaseRestClient(authedHttpClient);

        geminiApiClient = new GeminiApiClient();
        suggestionRepository = new SuggestionRepository(geminiApiClient);
        mealLogRepository = new MealLogRepository(restClient, tokenStorage);
        targetMacrosRepository = new TargetMacrosRepository(restClient, tokenStorage);
        userProfilesRepository = new UserProfilesRepository(restClient, tokenStorage);
    }

    public static ServiceLocator getInstance(Context context) {
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) {
                    instance = new ServiceLocator(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
}