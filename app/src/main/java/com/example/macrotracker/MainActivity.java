package com.example.macrotracker;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.macrotracker.data.RepoCallback;
import com.example.macrotracker.data.remote.GeminiApiClient;
import com.example.macrotracker.models.TargetMacros;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d("GeminiSmokeTest", "onCreate reached");

        try {
            Log.d("GeminiSmokeTest", "Creating GeminiApiClient");
            GeminiApiClient client = new GeminiApiClient();

            Log.d("GeminiSmokeTest", "Creating TargetMacros");
            TargetMacros target = new TargetMacros(
                    BigDecimal.valueOf(2000), BigDecimal.valueOf(150),
                    BigDecimal.valueOf(200), BigDecimal.valueOf(50));

            Log.d("GeminiSmokeTest", "Calling estimateMealWithSuggestion");
            client.estimateMealWithSuggestion(
                    "a bowl of oatmeal with a banana",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    target,
                    new RepoCallback<String>() {
                        @Override
                        public void onSuccess(String responseJson) {
                            Log.d("GeminiSmokeTest", "SUCCESS: " + responseJson);
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "Gemini OK — check Logcat", Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("GeminiSmokeTest", "onError fired", e);
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "Gemini FAILED: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });

            Log.d("GeminiSmokeTest", "Call dispatched, waiting for callback");

        } catch (Exception e) {
            Log.e("GeminiSmokeTest", "Crashed before dispatch", e);
        }
    }
}