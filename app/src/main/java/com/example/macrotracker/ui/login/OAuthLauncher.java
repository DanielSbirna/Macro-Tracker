package com.example.macrotracker.ui.login;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.macrotracker.R;
import com.example.macrotracker.data.ServiceLocator;
public class OAuthLauncher {

    public static final String GOOGLE = "google";
    public static final String FACEBOOK = "facebook";

    public static void launch(Context context, String provider) {
        String url = ServiceLocator.getInstance(context).authRepository.startOAuth(provider);
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.oauth_no_browser, Toast.LENGTH_LONG).show();
        }
    }
}
