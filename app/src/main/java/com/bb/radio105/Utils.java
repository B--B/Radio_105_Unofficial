package com.bb.radio105;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class Utils {
//    static boolean isRadioStreamingRunning(Context context) {
//        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (MusicService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }

    static boolean isRadioStreamingRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }

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
}
