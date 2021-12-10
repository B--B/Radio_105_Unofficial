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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class RadioFragment extends Fragment {

    private Button button1;
    private Button button2;
    private Button button3;
    private View root;
    private ImageView imageLogo;
    private ImageView imageArt;
    private TextView titleText;
    private TextView djNameText;
    private RadioServiceBinder mRadioServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    boolean mBound = false;
    private Intent startMusicService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        root = inflater.inflate(R.layout.fragment_radio, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().moveTaskToBack(true);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        imageLogo = root.findViewById(R.id.imageLogo);
        imageArt = root.findViewById(R.id.imageArt);
        titleText = root.findViewById(R.id.titleText);
        djNameText = root.findViewById(R.id.djNameText);
        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);
        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);

        return root;
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.
                beginTransaction()
                .detach(this)
                .commitNowAllowingStateLoss();
        fragmentManager.
                beginTransaction().
                attach(this).
                commitAllowingStateLoss();
    }

    @Override
    public void onStart() {
        startMusicService = new Intent(requireContext(), RadioService.class);
        requireContext().startService(startMusicService);
        super.onStart();
        requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
        if (RadioService.isPlaying) {
            imageArt.setVisibility(View.VISIBLE);
            imageLogo.setVisibility(View.INVISIBLE);
            titleText.setVisibility(View.VISIBLE);
            djNameText.setVisibility(View.VISIBLE);
        } else {
            imageArt.setVisibility(View.INVISIBLE);
            imageLogo.setVisibility(View.VISIBLE);
            titleText.setVisibility(View.INVISIBLE);
            djNameText.setVisibility(View.INVISIBLE);
        }
        buildTransportControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unbindService(mServiceConnection);
        mRadioServiceBinder = null;
        if (RadioService.mState == PlaybackStateCompat.STATE_STOPPED) {
            requireContext().stopService(startMusicService);
        }
        startMusicService = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat.unregisterCallback(mCallback);
            mMediaControllerCompat = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageArt = null;
        imageLogo = null;
        button1 = null;
        button2 = null;
        button3 = null;
        djNameText = null;
        titleText = null;
        root = null;
    }

    void buildTransportControls() {
        // Attach a listener to the button
        button1.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().play();
                Utils.fadeOutAndInImageView(imageLogo, imageArt);
                titleText.setText(mRadioServiceBinder.getTitleString());
                Utils.fadeInTextView(titleText);
                djNameText.setText(mRadioServiceBinder.getDjString());
                Utils.fadeInTextView(djNameText);
            }
        });
        button2.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().pause();
                Utils.fadeOutAndInImageView(imageArt, imageLogo);
                Utils.fadeOutTextView(titleText);
                Utils.fadeOutTextView(djNameText);
            }
        });
        button3.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().stop();
                if (imageArt.getVisibility() == View.VISIBLE) {
                    Utils.fadeOutAndInImageView(imageArt, imageLogo);
                    Utils.fadeOutTextView(titleText);
                    Utils.fadeOutTextView(djNameText);
                }
            }
        });
    }

    private void setButtonState() {
        if (mRadioServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            Drawable imageResource = new BitmapDrawable(getResources(), mRadioServiceBinder.getArt());
            imageArt.setImageDrawable(imageResource);
            titleText.setText(mRadioServiceBinder.getTitleString());
            djNameText.setText(mRadioServiceBinder.getDjString());
            button1.setEnabled(false);
            button2.setEnabled(true);
            button3.setEnabled(true);
        } else if (mRadioServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_PAUSED) {
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(true);
        } else if (mRadioServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_BUFFERING) {
            button1.setEnabled(false);
            button2.setEnabled(false);
            button3.setEnabled(false);
        } else if (mRadioServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_STOPPED) {
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        } else if (mRadioServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_ERROR) {
            if (imageArt.getVisibility() == View.VISIBLE) {
                Utils.fadeOutAndInImageView(imageArt, imageLogo);
                Utils.fadeOutTextView(titleText);
                Utils.fadeOutTextView(djNameText);
            }
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.e("Connection successful");
            mRadioServiceBinder = (RadioServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mRadioServiceBinder.getMediaSessionToken());
            mCallback.onPlaybackStateChanged(mMediaControllerCompat.getPlaybackState());
            mMediaControllerCompat.registerCallback(mCallback);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.e("Service crashed");
            mBound = false;
        }
    };

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Drawable imageResource = new BitmapDrawable(getResources(), mRadioServiceBinder.getArt());
            imageArt.setImageDrawable(imageResource);
            titleText.setText(mRadioServiceBinder.getTitleString());
            djNameText.setText(mRadioServiceBinder.getDjString());
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            setButtonState();
        }
    };
}
