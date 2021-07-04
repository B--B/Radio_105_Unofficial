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

import static android.content.Context.UI_MODE_SERVICE;

import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class TvFragment extends Fragment {

    private View root;
    private ProgressBar progressBar;
    private VideoView videoView;
    private String videoUrl;
    private MusicServiceBinder mMusicServiceBinder;
    boolean mBound = false;
    private MediaControllerCompat mMediaControllerCompat;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_tv, container, false);

        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        videoView = root.findViewById(R.id.videoView);
        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.setUpFullScreen(requireActivity());
        } else {
            Utils.restoreScreen(requireActivity());
        }

        progressBar.setVisibility(View.VISIBLE);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind music service
        requireContext().bindService(new Intent(getContext(), MusicService.class), mServiceConnection, 0);
        // Start video streaming
        videoUrl = "https://live2-radio-mediaset-it.akamaized.net/content/hls_h0_clr_vos/live/channel(ec)/index.m3u8";
        videoView.requestFocus();
        videoView.setOnInfoListener(onInfoToPlayStateListener);
        videoView.setVideoURI(Uri.parse(videoUrl));

        UiModeManager mUiModeManager = (UiModeManager) requireActivity().getSystemService(UI_MODE_SERVICE);
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean doNotAskAgain = mSharedPreferences.getBoolean("doNotAskAgain", false);

        if ((mUiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION) && !doNotAskAgain) {
            LayoutInflater mLayoutInflater = LayoutInflater.from(requireContext());
            View tvDialogLayout = mLayoutInflater.inflate(R.layout.tv_fragment_dialog, null);
            CheckBox dialogCheckBox = tvDialogLayout.findViewById(R.id.dontShowAgain);
            new AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setTitle(R.string.important)
                    .setView(tvDialogLayout)
                    .setNeutralButton(R.string.ok, (arg0, arg1) -> {
                        if (dialogCheckBox.isChecked()) {
                            SharedPreferences.Editor editor = mSharedPreferences.edit();
                            editor.putBoolean("doNotAskAgain", true);
                            editor.apply();
                        }
                        videoView.start();
                    })
                    .show();
        } else {
            videoView.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind music service
        requireContext().unbindService(mServiceConnection);
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.setUpFullScreen(requireActivity());
        } else {
            Utils.restoreScreen(requireActivity());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.restoreScreen(requireActivity());
        }
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMusicServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        videoView = null;
        progressBar = null;
        videoUrl = null;
        root = null;
    }

    private final MediaPlayer.OnInfoListener onInfoToPlayStateListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {
                progressBar.setVisibility(View.GONE);
            }
            if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what) {
                progressBar.setVisibility(View.VISIBLE);
            }
            return false;
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.e("Connection successful");
            mMusicServiceBinder = (MusicServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mMusicServiceBinder.getMediaSessionToken());
            mBound = true;
            // Stop radio streaming if running
            if (mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                mMediaControllerCompat.getTransportControls().pause();
            }
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.e("Service crashed");
            mBound = false;
        }
    };
}
