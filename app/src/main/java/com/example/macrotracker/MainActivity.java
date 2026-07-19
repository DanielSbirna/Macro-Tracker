package com.example.macrotracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.macrotracker.data.ServiceLocator;

// black color scheme android status bar
import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ServiceLocator.getInstance(this).authRepository.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // black scheme status bar impl
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new FragmentHome())
                    .commit();
        }

        setupBottomNav();
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            showFragment(new FragmentHome());
            setNavSelected(v);
        });
        findViewById(R.id.navFoodLog).setOnClickListener(v -> {
            showFragment(new FoodLogFragment());
            setNavSelected(v);
        });
        //navAccount not wired yet - no destination fragments for those yet

        // Set initial highlight for default fragment
        setNavSelected(findViewById(R.id.navHome));

        View addButton = findViewById(R.id.addButton);
        if (addButton != null) {
            addButton.setOnClickListener(v -> {
                AddDrawerFragment addDrawer = new AddDrawerFragment();
                addDrawer.show(getSupportFragmentManager(), "AddDrawerFragment");
            });
        }

        findViewById(R.id.navAssistant).setOnClickListener(v -> {
            showFragment(new AssistantFragment());
            setNavSelected(v);
        });

    }

    private void setNavSelected(View selected) {
        ImageButton[] navButtons = {
                findViewById(R.id.navHome), findViewById(R.id.navFoodLog),
                findViewById(R.id.navAssistant), findViewById(R.id.navAccount)
        };
        for (ImageButton btn : navButtons) {
            if (btn != null) {
                boolean isSelected = btn == selected;
                btn.setBackgroundResource(isSelected ? R.drawable.bg_nav_selected : android.R.color.transparent);
                if (isSelected) {
                    btn.setColorFilter(android.graphics.Color.WHITE);
                } else {
                    btn.clearColorFilter();
                }
            }
        }
    }

    private void showFragment(Fragment fragment) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current != null && current.getClass().equals(fragment.getClass())) return;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void navigateToAssistant() {
        showFragment(new AssistantFragment());
        setNavSelected(findViewById(R.id.navAssistant));
    }
}