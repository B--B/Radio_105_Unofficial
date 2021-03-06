package com.bb.radio105;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements  UpdateColorsInterface {

    private AppBarConfiguration mAppBarConfiguration;
    static UpdateColorsInterface updateColorsInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String pref = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (pref.equals(darkModeValues[0]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (pref.equals(darkModeValues[1]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (pref.equals(darkModeValues[2]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_tv, R.id.nav_podcast, R.id.nav_zoo, R.id.nav_socials, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        updateColorsInterface = this;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onUpdate(boolean zooColors) {
        if (zooColors) {
            setZooColors();
        } else {
            setStockColors();
        }
    }

    void setZooLightColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.zoo_300));
        Objects.requireNonNull(((AppCompatActivity) this).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.zoo_500)));
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar_zoo);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(this, R.color.black),
                        ContextCompat.getColor(this, R.color.zoo_500),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    void setZooDarkColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.zoo_200));
        Objects.requireNonNull(((AppCompatActivity) this).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.zoo_500)));
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar_zoo);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(this, R.color.white),
                        ContextCompat.getColor(this, R.color.zoo_500),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    void setStockLightColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.orange_900));
        Objects.requireNonNull(((AppCompatActivity) this).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.yellow_700)));
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(this, R.color.black),
                        ContextCompat.getColor(this, R.color.yellow_700),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    void setStockDarkColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.yellow_700));
        Objects.requireNonNull(((AppCompatActivity) this).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.yellow_200)));
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(this, R.color.white),
                        ContextCompat.getColor(this, R.color.yellow_200)
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    void setZooColors() {
        final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String themePref = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (themePref.equals(darkModeValues[0])) {
            // Check system status and apply colors
            int nightModeOn = getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeOn) {
                case Configuration.UI_MODE_NIGHT_YES:
                    setZooDarkColors();
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    setZooLightColors();
                    break;
            }
        }
        if (themePref.equals(darkModeValues[1])) {
            setZooLightColors();
        }
        if (themePref.equals(darkModeValues[2])) {
            setZooDarkColors();
        }
    }

    void setStockColors() {
        final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String themePref = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (themePref.equals(darkModeValues[0])) {
            // Check system status and apply colors
            int nightModeOn = getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeOn) {
                case Configuration.UI_MODE_NIGHT_YES:
                    setStockDarkColors();
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    setStockLightColors();
                    break;
            }
        }
        if (themePref.equals(darkModeValues[1])) {
            setStockLightColors();
        }
        if (themePref.equals(darkModeValues[2])) {
            setStockDarkColors();
        }
    }
}
