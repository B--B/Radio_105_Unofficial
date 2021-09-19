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
    private MusicServiceBinder mMusicServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    boolean mBound = false;
    private Intent startMusicService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        startMusicService = new Intent(requireContext(), RadioService.class);
        requireContext().startService(startMusicService);

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
                commitNowAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
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
        mMusicServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat.unregisterCallback(mCallback);
            mMediaControllerCompat = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMusicServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat.unregisterCallback(mCallback);
            mMediaControllerCompat = null;
        }
        imageArt = null;
        imageLogo = null;
        button1 = null;
        button2 = null;
        button3 = null;
        djNameText = null;
        titleText = null;
        root = null;
        if (RadioService.mState == PlaybackStateCompat.STATE_STOPPED) {
            requireContext().stopService(startMusicService);
        }
        startMusicService = null;
    }

    void buildTransportControls() {
        // Attach a listener to the button
        button1.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().play();
            }
        });
        button2.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().pause();
            }
        });
        button3.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().stop();
            }
        });
    }

    private void setButtonState() {
        if (mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            imageLogo.setVisibility(View.GONE);
            Drawable imageResource = new BitmapDrawable(getResources(), mMusicServiceBinder.getArt());
            imageArt.setImageDrawable(imageResource);
            imageArt.setVisibility(View.VISIBLE);
            titleText.setText(mMusicServiceBinder.getTitleString());
            titleText.setVisibility(View.VISIBLE);
            djNameText.setText(mMusicServiceBinder.getDjString());
            djNameText.setVisibility(View.VISIBLE);
            button1.setEnabled(false);
            button2.setEnabled(true);
            button3.setEnabled(true);
        } else if (mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_PAUSED) {
            imageLogo.setVisibility(View.VISIBLE);
            imageArt.setVisibility(View.GONE);
            titleText.setVisibility(View.GONE);
            djNameText.setVisibility(View.GONE);
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(true);
        } else if (mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_STOPPED || mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_ERROR) {
            imageLogo.setVisibility(View.VISIBLE);
            imageArt.setVisibility(View.GONE);
            titleText.setVisibility(View.GONE);
            djNameText.setVisibility(View.GONE);
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        } else if (mMusicServiceBinder.getPlaybackState() == PlaybackStateCompat.STATE_BUFFERING) {
            imageLogo.setVisibility(View.VISIBLE);
            imageArt.setVisibility(View.GONE);
            titleText.setVisibility(View.GONE);
            djNameText.setVisibility(View.GONE);
            button1.setEnabled(false);
            button2.setEnabled(false);
            button3.setEnabled(false);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.e("Connection successful");
            mMusicServiceBinder = (MusicServiceBinder) service;
            mMediaControllerCompat = new MediaControllerCompat(getContext(), mMusicServiceBinder.getMediaSessionToken());
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
            Drawable imageResource = new BitmapDrawable(getResources(), mMusicServiceBinder.getArt());
            imageArt.setImageDrawable(imageResource);
            titleText.setText(mMusicServiceBinder.getTitleString());
            djNameText.setText(mMusicServiceBinder.getDjString());
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            setButtonState();
        }
    };
}
