package com.example.macrotracker.data;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
public class JwtUtils {

    public static String getUserIdFromToken(String accessToken) throws JSONException {
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Malformed JWT");
        }

        String payloadSegment = addPadding(parts[1]);
        byte[] decoded = Base64.decode(payloadSegment, Base64.URL_SAFE | Base64.NO_WRAP);
        String payload = new String(decoded, StandardCharsets.UTF_8);

        return new JSONObject(payload).getString("sub");
    }

    private static String addPadding(String base64Url) {
        int remainder = base64Url.length() % 4;
        if (remainder == 0) return base64Url;
        return base64Url + "====".substring(remainder);
    }
}

