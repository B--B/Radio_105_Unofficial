package com.bb.radio105;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class Utils {
    /**
     * Requests the {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    static void requestStoragePermission(Activity mActivity, View mView) {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity ,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mView, R.string.storage_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, view -> {
                // Request the permission
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }).show();
        } else {
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    static void startDownload(Activity mActivity, String mString) {
        DownloadManager downloadManager = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(mString);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(mActivity.getString(R.string.menu_home));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());
        downloadManager.enqueue(request);
    }

    static void setUpFullScreen(Activity mActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = mActivity.getWindow().getInsetsController();

            if (controller != null)
                controller.hide(WindowInsets.Type.statusBars());
        } else {
            mActivity.getWindow().addFlags(FLAG_FULLSCREEN);
        }
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar()).hide();
    }

    static void restoreScreen(Activity mActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = mActivity.getWindow().getInsetsController();

            if (controller != null)
                controller.show(WindowInsets.Type.statusBars());
        } else {
            mActivity.getWindow().clearFlags(FLAG_FULLSCREEN);
        }
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar()).show();
    }

    static void setZooLightColors(Activity mActivity) {
        mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.zoo_300));
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(mActivity, R.color.zoo_500)));
        NavigationView mNavigationView = mActivity.findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar_zoo);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(mActivity, R.color.black),
                        ContextCompat.getColor(mActivity, R.color.zoo_500),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    static void setZooDarkColors(Activity mActivity) {
        mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.zoo_200));
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(mActivity, R.color.zoo_500)));
        NavigationView mNavigationView = mActivity.findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar_zoo);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(mActivity, R.color.white),
                        ContextCompat.getColor(mActivity, R.color.zoo_500),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    static void setStockLightColors(Activity mActivity) {
        mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.orange_900));
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(mActivity, R.color.yellow_700)));
        NavigationView mNavigationView = mActivity.findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(mActivity, R.color.black),
                        ContextCompat.getColor(mActivity, R.color.yellow_700),
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    static void setStockDarkColors(Activity mActivity) {
        mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.yellow_700));
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar())
                .setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(mActivity, R.color.yellow_200)));
        NavigationView mNavigationView = mActivity.findViewById(R.id.nav_view);
        View header = mNavigationView.getHeaderView(0);
        LinearLayout mLinearLayout = header.findViewById(R.id.nav_header);
        mLinearLayout.setBackgroundResource(R.drawable.side_nav_bar);
        ColorStateList mColorStateList = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_checked},
                        new int[] { android.R.attr.state_checked}
                },
                new int[] {
                        ContextCompat.getColor(mActivity, R.color.white),
                        ContextCompat.getColor(mActivity, R.color.yellow_200)
                }
        );
        mNavigationView.setItemIconTintList(mColorStateList);
        mNavigationView.setItemTextColor(mColorStateList);
    }

    static void setZooColors(Activity mActivity) {
        final String[] darkModeValues = mActivity.getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String themePref = PreferenceManager.getDefaultSharedPreferences(mActivity)
                .getString(mActivity.getString(R.string.theme_key), mActivity.getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (themePref.equals(darkModeValues[0])) {
            // Check system status and apply colors
            int nightModeOn = mActivity.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeOn) {
                case Configuration.UI_MODE_NIGHT_YES:
                    setZooDarkColors(mActivity);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    setZooLightColors(mActivity);
                    break;
            }
        }
        if (themePref.equals(darkModeValues[1])) {
            setZooLightColors(mActivity);
        }
        if (themePref.equals(darkModeValues[2])) {
            setZooDarkColors(mActivity);
        }
    }

    static void setStockColors(Activity mActivity) {
        final String[] darkModeValues = mActivity.getResources().getStringArray(R.array.theme_values);
        // The apps theme is decided depending upon the saved preferences on app startup
        String themePref = PreferenceManager.getDefaultSharedPreferences(mActivity)
                .getString(mActivity.getString(R.string.theme_key), mActivity.getString(R.string.theme_default_value));
        // Comparing to see which preference is selected and applying those theme settings
        if (themePref.equals(darkModeValues[0])) {
            // Check system status and apply colors
            int nightModeOn = mActivity.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeOn) {
                case Configuration.UI_MODE_NIGHT_YES:
                    setStockDarkColors(mActivity);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    setStockLightColors(mActivity);
                    break;
            }
        }
        if (themePref.equals(darkModeValues[1])) {
            setStockLightColors(mActivity);
        }
        if (themePref.equals(darkModeValues[2])) {
            setStockDarkColors(mActivity);
        }
    }
}
