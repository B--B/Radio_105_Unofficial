/*
 * Copyright (C) 2021 Zanin Marco (B--B)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bb.radio105;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;
import org.adblockplus.libadblockplus.android.webview.BuildConfig;

import java.util.Objects;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements  UpdateColorsInterface, ActivityCompat.OnRequestPermissionsResultCallback {

    private AppBarConfiguration mAppBarConfiguration;
    static UpdateColorsInterface updateColorsInterface;
    static Boolean isZooColor = false;

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
                R.id.nav_home, R.id.nav_tv, R.id.nav_podcast, R.id.nav_zoo, R.id.nav_socials, R.id.nav_settings, R.id.nav_developer)
                .setOpenableLayout(drawer)
                .build();
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        updateColorsInterface = this;

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean isFirstStart = mSharedPreferences.getBoolean("firstStart", true);

        if (isFirstStart) {
            new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle(R.string.disclaimer_title)
                    .setMessage(R.string.disclaimer)
                    .setNeutralButton(R.string.ok, (arg0, arg1) -> {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean("firstStart", false);
                        editor.apply();
                    })
                    .show();
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            AdblockWebView.setWebContentsDebuggingEnabled(true);
        }

        // Start the service worker controller here, actually only an instance is allowed, but
        // we have two fragments that runs webView. In addition the service worker controller must be
        // started BEFORE the webView instance, and in this way webView cannot start before the service.
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ServiceWorkerController mServiceWorkerController = ServiceWorkerController.getInstance();
            mServiceWorkerController.setServiceWorkerClient(new ServiceWorkerClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    Log.e(Constants.LOG_TAG, "in service worker. isMainFrame:"+request.isForMainFrame() +": " + request.getUrl());
                    return null;
                }
            });
            mServiceWorkerController.getServiceWorkerWebSettings().setAllowContentAccess(true);
            mServiceWorkerController.getServiceWorkerWebSettings().setAllowFileAccess(true);
        } */
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
            if (isZooColor) {
                setStockColors();
                isZooColor = false;
            }
        }
    }

    @Override
    public void onDestroy() {
        updateColorsInterface = null;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == Constants.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // Request for storage permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                Snackbar.make(findViewById(R.id.drawer_layout), R.string.storage_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                // Permission request was denied.
                Snackbar.make(findViewById(R.id.drawer_layout), R.string.storage_permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setZooLightColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.zoo_300));
        Objects.requireNonNull(this.getSupportActionBar())
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

    private void setZooDarkColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.zoo_200));
        Objects.requireNonNull(this.getSupportActionBar())
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

    private void setStockLightColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.orange_900));
        Objects.requireNonNull(this.getSupportActionBar())
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

    private void setStockDarkColors() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.yellow_700));
        Objects.requireNonNull(this.getSupportActionBar())
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

    private void setZooColors() {
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

    private void setStockColors() {
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
