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
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class TvFragment extends Fragment {

    private View root;
    private ProgressBar progressBar;
    private VideoView videoView;
    private RadioServiceBinder mRadioServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    ConstraintLayout mConstraintLayout;
    static boolean isTvPlaying;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_tv, container, false);

        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mConstraintLayout  = root.findViewById(R.id.tvFragment);
        progressBar = root.findViewById(R.id.progressBar);

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
        // Bind music service only if is already running
        if (RadioService.mState != STATE_STOPPED) {
            requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
        }
        // Create the VideoView and set the size
        videoView = new VideoView(requireContext());
        videoView.setId(VideoView.generateViewId());
        mConstraintLayout.addView(videoView);
        setVideoViewSize();
        // Start video streaming
        final String videoUrl = "https://live2-radio-mediaset-it.akamaized.net/content/hls_h0_clr_vos/live/channel(ec)/index.m3u8";
        videoView.requestFocus();
        videoView.setOnPreparedListener(onPreparedListener);
        videoView.setVideoURI(Uri.parse(videoUrl));

        UiModeManager mUiModeManager = (UiModeManager) requireActivity().getSystemService(UI_MODE_SERVICE);
        boolean doNotAskAgain = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.do_not_show_again_key), false);

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
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                            editor.putBoolean(getString(R.string.do_not_show_again_key), true);
                            editor.apply();
                        }
                        videoView.start();
                    })
                    .show();
        } else {
            videoView.start();
        }
        isTvPlaying = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (RadioService.mState != STATE_STOPPED) {
            // Unbind music service
            requireContext().unbindService(mServiceConnection);
        }
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        isTvPlaying = false;
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setVideoViewLandscapeSize();
            Utils.setUpFullScreen(requireActivity());
        } else {
            setVideoViewPortraitSize();
            Utils.restoreScreen(requireActivity());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Utils.setUpFullScreen(requireActivity());
            }
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Utils.restoreScreen(requireActivity());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.restoreScreen(requireActivity());
        }
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mRadioServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        videoView.stopPlayback();
        videoView = null;
        progressBar = null;
        root = null;
    }

    private final MediaPlayer.OnPreparedListener onPreparedListener = mediaPlayer -> progressBar.setVisibility(View.GONE);

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.e("Connection successful");
            mRadioServiceBinder = (RadioServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mRadioServiceBinder.getMediaSessionToken());
            // Stop radio streaming if running
            if (RadioService.mState == STATE_PLAYING) {
                mMediaControllerCompat.getTransportControls().pause();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.e("Service crashed");
        }
    };

    private void setVideoViewLandscapeSize() {
        ConstraintLayout.LayoutParams mLayoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        videoView.setLayoutParams(mLayoutParams);
        ConstraintSet set = new ConstraintSet();
        set.clone(mConstraintLayout);
        set.connect(videoView.getId(), ConstraintSet.TOP, mConstraintLayout.getId(), ConstraintSet.TOP, 0);
        set.connect(videoView.getId(), ConstraintSet.BOTTOM, mConstraintLayout.getId(), ConstraintSet.BOTTOM, 0);
        set.connect(videoView.getId(), ConstraintSet.LEFT, mConstraintLayout.getId(), ConstraintSet.LEFT, 0);
        set.connect(videoView.getId(), ConstraintSet.RIGHT, mConstraintLayout.getId(), ConstraintSet.RIGHT, 0);
        set.applyTo(mConstraintLayout);
    }

    private void setVideoViewPortraitSize() {
        ConstraintLayout.LayoutParams mLayoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        videoView.setLayoutParams(mLayoutParams);
        ConstraintSet set = new ConstraintSet();
        set.clone(mConstraintLayout);
        set.connect(videoView.getId(), ConstraintSet.TOP, mConstraintLayout.getId(), ConstraintSet.TOP, 0);
        set.connect(videoView.getId(), ConstraintSet.BOTTOM, mConstraintLayout.getId(), ConstraintSet.BOTTOM, 0);
        set.connect(videoView.getId(), ConstraintSet.LEFT, mConstraintLayout.getId(), ConstraintSet.LEFT, 0);
        set.connect(videoView.getId(), ConstraintSet.RIGHT, mConstraintLayout.getId(), ConstraintSet.RIGHT, 0);
        set.applyTo(mConstraintLayout);
    }

    private void setVideoViewSize() {
        if (requireContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setVideoViewLandscapeSize();
        } else {
            setVideoViewPortraitSize();
        }
    }
}
