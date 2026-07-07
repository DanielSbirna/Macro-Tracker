package com.example.macrotracker;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // test api ai
//        GeminiApiClient client = new GeminiApiClient();
//
//        client.estimateMacros("a bowl of oatmeal with a banana", new MacroCallback() {
//            @Override
//            public void onSuccess(String responseJson) {
//                Log.d("MacroTest", responseJson);
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Log.e("MacroTest", "Error", e);
//            }
//        });

        // test api db

//        Log.d("AuthTest", "Starting signup test");
//
//        SupabaseAuthClient authClient = new SupabaseAuthClient();
//
//        authClient.signUp("test@gmail.com", "testpassword123", new AuthCallback() {
//            @Override
//            public void onSuccess(String accessToken) {
//                Log.d("AuthTest", accessToken);
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Log.e("AuthTest", "Error", e);
//            }
//        });
    }
}