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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class TvFragment extends Fragment {

    private View root;
    private ProgressBar progressBar;
    private VideoView videoView;
    private RadioServiceBinder mRadioServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    private FloatingActionButton mFloatingActionButton;
    ConstraintLayout mConstraintLayout;
    static boolean isTvPlaying;
    private Boolean isFabVisible = false;
    private Boolean userManuallyRotateScreen = false;
    private OrientationEventListener mOrientationEventListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_tv, container, false);

        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mConstraintLayout  = root.findViewById(R.id.tvFragment);
        progressBar = root.findViewById(R.id.progressBar);
        mFloatingActionButton = root.findViewById(R.id.rotationButton);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.setUpFullScreen(requireActivity());
        } else {
            Utils.restoreScreen(requireActivity());
        }

        progressBar.setVisibility(View.VISIBLE);

        return root;
    }

    // SourceLockedOrientationActivity is a false positive warn, SCREEN_ORIENTATION_USER is set when the fragment is destroyed
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onStart() {
        super.onStart();
        // Bind music service only if is already running
        if (RadioService.mState != STATE_STOPPED) {
            requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
        }
        // Set the background here avoid wrong color in some cases.
        // Example: Enter PiP mode -> Exit PiP mode -> Enter app from recent tasks -> Background is black
        mConstraintLayout.setBackgroundColor(getThemeBackgroundColor());
        // Set the rotation button...
        mFloatingActionButton.setOnClickListener(view -> {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                userManuallyRotateScreen = true;
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                userManuallyRotateScreen = true;
            }
        });
        // And the rotation listener.
        mOrientationEventListener = new OrientationEventListener(getActivity()) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        int epsilon = 10;
                        int portrait = 0;
                        int leftLandscape = 90;
                        int reversePortrait = 180;
                        int rightLandscape = 270;
                        if ((epsilonCheck(orientation, leftLandscape, epsilon) ||
                                epsilonCheck(orientation, rightLandscape, epsilon)) && requireActivity().getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE) {
                            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                        }
                        if ((epsilonCheck(orientation, portrait, epsilon) ||
                                epsilonCheck(orientation, reversePortrait, epsilon)) && requireActivity().getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT) {
                            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                        }
                    }
                    private boolean epsilonCheck(int a, int b, int epsilon) {
                        return a > b - epsilon && a < b + epsilon;
                    }
                };
        mOrientationEventListener.enable();
        // Create the VideoView and set the size
        videoView = new VideoView(requireContext());
        videoView.setId(VideoView.generateViewId());
        // VideoView will never start if is invisible, a minimum size must be set
        ConstraintLayout.LayoutParams mLayoutParams = new ConstraintLayout.LayoutParams(1, 0);
        videoView.setLayoutParams(mLayoutParams);
        mConstraintLayout.addView(videoView);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        videoView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                             oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || right != oldRight || top != oldTop
                    || bottom != oldBottom) {
                // The videoView’s bounds changed, update the source hint rect to
                // reflect its new bounds.
                final Rect sourceRectHint = new Rect();
                videoView.getGlobalVisibleRect(sourceRectHint);
                    requireActivity().setPictureInPictureParams(
                            new PictureInPictureParams.Builder()
                                    .setSourceRectHint(sourceRectHint)
                                    .build());
                }
            });
        }

        if (mUiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION) {
            mFloatingActionButton.setAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in));
            mFloatingActionButton.setVisibility(View.VISIBLE);
            isFabVisible = true;
            root.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isFabVisible) {
                        mFloatingActionButton.setAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in));
                        mFloatingActionButton.setVisibility(View.VISIBLE);
                        isFabVisible = true;
                    } else {
                        mFloatingActionButton.setAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out));
                        mFloatingActionButton.setVisibility(View.INVISIBLE);
                        isFabVisible = false;
                    }
                }
                view.performClick();
                return true;
            });
        }
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
        mOrientationEventListener.disable();
        mOrientationEventListener = null;
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
        if (userManuallyRotateScreen) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
        mRadioServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat = null;
        }
        videoView.stopPlayback();
        videoView = null;
        progressBar = null;
        root = null;
    }

    private final MediaPlayer.OnPreparedListener onPreparedListener = mediaPlayer -> {
        progressBar.setVisibility( View.GONE );
        setVideoViewSize();
        int blackColor = ContextCompat.getColor(requireContext(), R.color.black);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), getThemeBackgroundColor(), blackColor);
        colorAnimation.setDuration(500);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                mConstraintLayout.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    };

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

    /**
     * Get the background color of the theme used for this activity.
     *
     * @return The background color of the current theme.
     */
    public int getThemeBackgroundColor() {
        TypedArray array = requireActivity().getTheme().obtainStyledAttributes(
                new int[] {
                        android.R.attr.colorBackground
                });
        int backgroundColor = array.getColor(0, 0);
        array.recycle();
        return backgroundColor;
    }
}
