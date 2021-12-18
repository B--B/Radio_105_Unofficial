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

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

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
        request.setTitle(mActivity.getString(R.string.app_name));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            request.allowScanningByMediaScanner();
        }
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());
        downloadManager.enqueue(request);
    }

    static void setUpFullScreen(Activity mActivity) {
        mActivity.getWindow().addFlags(SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(mActivity.getWindow(), mActivity.getWindow().getDecorView());
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars());
        controllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar()).hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mActivity.getWindow().setDecorFitsSystemWindows(false);
        }
    }

    static void restoreScreen(Activity mActivity) {
        mActivity.getWindow().clearFlags(SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(mActivity.getWindow(), mActivity.getWindow().getDecorView());
        controllerCompat.show(WindowInsetsCompat.Type.systemBars());
        controllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE);
        Objects.requireNonNull(((AppCompatActivity) mActivity).getSupportActionBar()).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mActivity.getWindow().setDecorFitsSystemWindows(true);
        }
    }

    static boolean isMiUi() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    static boolean isEMUI() {
        return !TextUtils.isEmpty(getSystemProperty("ro.build.version.emui"));
    }

    static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    static void fadeOutAndInImageView(final ImageView fadeOutImageView, final ImageView fadeInImageView)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(500);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(500);

        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeOutImageView.setVisibility(View.INVISIBLE);
                fadeInImageView.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation) {
                fadeInImageView.setVisibility(View.VISIBLE);}

            @Override
            public void onAnimationEnd(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        fadeOutImageView.startAnimation(fadeOut);
    }


    static void fadeOutTextView(final TextView mTextView)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(1000);

        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mTextView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mTextView.startAnimation(fadeOut);
    }

    static void fadeInTextView(final TextView mTextView)
    {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(1000);

        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation) {
                mTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mTextView.startAnimation(fadeIn);
    }
}
