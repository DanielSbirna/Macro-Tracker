package com.example.macrotracker;

import com.example.macrotracker.data.SupabaseCallback;
import com.example.macrotracker.data.SupabaseRestClient;
import com.example.macrotracker.models.TargetMacros;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NetworkIntegrationTest {

    @Test
    public void testGeminiApiIntegration() throws InterruptedException {
        GeminiApiClient client = new GeminiApiClient();

        // 1. Latch allows the test to wait for the background thread to finish
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = {null};
        final Exception[] error = {null};

        // 2. Perform the actual call
        client.estimateMealWithSuggestion(
                "a bowl of oatmeal with a banana",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new TargetMacros(BigDecimal.valueOf(2000), BigDecimal.valueOf(150), BigDecimal.valueOf(200), BigDecimal.valueOf(50)),
                new MacroCallback() {
                    @Override
                    public void onSuccess(String responseJson) {
                        result[0] = responseJson;
                        latch.countDown(); // Signal that we are done
                    }

                    @Override
                    public void onError(Exception e) {
                        error[0] = e;
                        latch.countDown(); // Signal that we are done (even if error)
                    }
                });

        // 3. Wait up to 10 seconds for the response
        boolean completed = latch.await(10, TimeUnit.SECONDS);

        // 4. Assertions
        assertTrue("Test timed out before response was received", completed);
        assertNull("API returned an error: " + (error[0] != null ? error[0].getMessage() : ""), error[0]);
        assertNotNull("Response was null", result[0]);

        System.out.println("Gemini Success! Response: " + result[0]);
    }
    @Test
    public void testSupabaseSelectIntegration() throws InterruptedException {
        // Ensure you replace "your_table_name" with an actual table in your DB
        SupabaseRestClient dbClient = new SupabaseRestClient(new OkHttpClient());
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = {null};

        dbClient.select("meals", null, new SupabaseCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                result[0] = jsonResponse;
                latch.countDown();
            }
            @Override
            public void onError(Exception e) {
                fail("Supabase Error: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS));
        assertNotNull("Database returned null", result[0]);
        System.out.println("Supabase Response: " + result[0]);
    }
}