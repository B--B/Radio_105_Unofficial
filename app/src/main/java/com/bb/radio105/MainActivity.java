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

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private AppBarConfiguration mAppBarConfiguration;
    private ReviewManager reviewManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash Screen
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        boolean isDebuggingEnabled = Utils.getUserPreferenceBoolean(getBaseContext(), getString(R.string.debug_key), false);

        if (BuildConfig.DEBUG || isDebuggingEnabled) {
            Timber.plant(new DebugTree());
        }

        Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();

        // Enable dynamic colors
        DynamicColors.applyToActivityIfAvailable(this);

        final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String darkMode = Utils.getUserPreferenceString(this, getString(R.string.theme_key), getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (darkMode.equals(darkModeValues[0]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (darkMode.equals(darkModeValues[1]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (darkMode.equals(darkModeValues[2]))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // NOTE: For some unknown reason splashscreen 1.0.0 lib breaks status bar dynamic color,
        // and can be worked around only setting programmatically status bar color.
        // This is probably caused by a bug in Android splashscreen lib, and can be reproduced everytime creating a new
        // Navigation Drawer View project and adding dynamic colors support and splashscreen following Google guidelines
        statusBarHax();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.setFitsSystemWindows(true);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
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

        boolean isFirstStart = Utils.getUserPreferenceBoolean(getBaseContext(), getString(R.string.is_first_start_key), true);

        if (isFirstStart) {
            new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle(R.string.disclaimer_title)
                    .setMessage(R.string.disclaimer)
                    .setNeutralButton(R.string.ok, (arg0, arg1) -> {
                        editor.putBoolean(getString(R.string.is_first_start_key), false);
                        editor.apply();
                    })
                    .show();
        }

        int openCount = Utils.getUserPreferenceInt(getBaseContext(), getString(R.string.app_open_count_key));
        Timber.e("App open count: %s", openCount);
        if (openCount == 10) {
            Timber.e("Showing Play Store rate dialog");
            showRateApp();
            editor.putInt(getString(R.string.app_open_count_key), 0).apply();
        } else {
            editor.putInt(getString(R.string.app_open_count_key), ++openCount).apply();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterComponentCallbacks(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onUserLeaveHint(){
        super.onUserLeaveHint();
        Context mContext = this;
        PackageManager mPackageManager = mContext.getPackageManager();
        if (PodcastFragment.isVideoInFullscreen || ZooFragment.isVideoInFullscreen || TvFragment.isTvPlaying) {
            if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                if (!isInPictureInPictureMode()) {
                    Intent intent = new Intent();
                    final List<RemoteAction> dummyRemoteActions = new ArrayList<>();
                    final RemoteAction dummyAction = new RemoteAction(Icon.createWithResource(this, R.drawable.ic_blank), getString(R.string.blank_line), getString(R.string.blank_line), PendingIntent.getBroadcast(this, 234, intent, PendingIntent.FLAG_IMMUTABLE));
                    dummyRemoteActions.add(dummyAction);
                    enterPictureInPictureMode(new PictureInPictureParams.Builder()
                            .setActions(dummyRemoteActions)
                            .setAspectRatio(new Rational(16, 9))
                            .build());
                }
            }
        }
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
        else if (requestCode == Constants.PERMISSION_REQUEST_POST_NOTIFICATIONS) {
            // Request for storage permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                Snackbar.make(findViewById(R.id.drawer_layout), R.string.notification_permission_granted,
                                Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                // Permission request was denied.
                Snackbar.make(findViewById(R.id.drawer_layout), R.string.notification_permission_denied,
                                Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void statusBarHax() {
        TypedValue mTypedValue = new TypedValue();
        Resources.Theme mTheme = this.getTheme();
        mTheme.resolveAttribute(R.attr.colorPrimary, mTypedValue, true);
        @ColorInt int mColor = mTypedValue.data;
        Window mWindow = this.getWindow();
        mWindow.setStatusBarColor(mColor);
    }

    private void showRateApp() {
        reviewManager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task1 -> Timber.e("Successfully completed rating activity"));
            } else {
                Timber.e("Review Error: %s", Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }
}
