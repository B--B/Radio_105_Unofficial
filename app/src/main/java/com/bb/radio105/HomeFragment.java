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

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.UI_MODE_SERVICE;

import android.app.UiModeManager;
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

import static com.bb.radio105.MusicService.mState;

public class HomeFragment extends Fragment {

    private Button button1;
    private Button button2;
    private Button button3;
    private View root;
    private ImageView imageLogo;
    private ImageView imageArt;
    private TextView titleText;
    private TextView djNameText;
    private MusicService.MusicServiceBinder mMusicServiceBinder;
    private ServiceConnection mServiceConnection;
    private MediaControllerCompat mMediaControllerCompat;
    private MediaControllerCompat.Callback mCallback;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Android TV
        UiModeManager uiModeManager = (UiModeManager) requireActivity().getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            root = inflater.inflate(R.layout.fragment_home_land_television, container, false);
        } else {
            root = inflater.inflate(R.layout.fragment_home, container, false);
        }

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

        mCallback = new MediaControllerCompat.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                Drawable imageResource = new BitmapDrawable(getResources(), MusicService.art);
                imageArt.setImageDrawable(imageResource);
                titleText.setText(MusicService.titleString);
                djNameText.setText(MusicService.djString);
            }

            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                setButtonState();
            }
        };

        // Finish building the UI
        buildTransportControls();

        return root;
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.
                beginTransaction()
                .detach(this)
                .commitAllowingStateLoss();
        super.onConfigurationChanged(newConfig);
        fragmentManager.
                beginTransaction().
                attach(this).
                commitAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
        setButtonState();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMusicServiceBinder = null;
        if (mMediaControllerCompat != null) {
            mMediaControllerCompat.unregisterCallback(mCallback);
            mMediaControllerCompat = null;
        }
        requireContext().unbindService(mServiceConnection);
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

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mMusicServiceBinder = (MusicService.MusicServiceBinder) service;
                mMediaControllerCompat = new MediaControllerCompat(getContext(), mMusicServiceBinder.getMediaSessionToken());
                mCallback.onPlaybackStateChanged(mMediaControllerCompat.getPlaybackState());
                mMediaControllerCompat.registerCallback(mCallback);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mMusicServiceBinder = null;
                if (mMediaControllerCompat != null) {
                    mMediaControllerCompat.unregisterCallback(mCallback);
                    mMediaControllerCompat = null;
                }
            }
        };

        requireContext().bindService(new Intent(getContext(), MusicService.class), mServiceConnection, BIND_AUTO_CREATE);

        // Attach a listener to the button
        button1.setOnClickListener(v -> {
            // If we are in a stopped state MusicService must be started
            if (mState == PlaybackStateCompat.STATE_STOPPED) {
                requireActivity().startService(new Intent(getContext(), MusicService.class));
            }
            mMediaControllerCompat.getTransportControls().play();
        });
        button2.setOnClickListener(v -> mMediaControllerCompat.getTransportControls().pause());
        button3.setOnClickListener(v -> mMediaControllerCompat.getTransportControls().stop());
    }

    private void setButtonState() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            imageLogo.setVisibility(View.GONE);
            Drawable imageResource = new BitmapDrawable(getResources(), MusicService.art);
            imageArt.setImageDrawable(imageResource);
            imageArt.setVisibility(View.VISIBLE);
            titleText.setText(MusicService.titleString);
            titleText.setVisibility(View.VISIBLE);
            djNameText.setText(MusicService.djString);
            djNameText.setVisibility(View.VISIBLE);
            button1.setEnabled(false);
            button2.setEnabled(true);
            button3.setEnabled(true);
        } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
            imageLogo.setVisibility(View.VISIBLE);
            imageArt.setVisibility(View.GONE);
            titleText.setVisibility(View.GONE);
            djNameText.setVisibility(View.GONE);
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(true);
        } else if (mState == PlaybackStateCompat.STATE_STOPPED || mState == PlaybackStateCompat.STATE_ERROR) {
            imageLogo.setVisibility(View.VISIBLE);
            imageArt.setVisibility(View.GONE);
            titleText.setVisibility(View.GONE);
            djNameText.setVisibility(View.GONE);
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        } else if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            imageLogo.setVisibility(View.VISIBLE);
            imageArt.setVisibility(View.GONE);
            titleText.setVisibility(View.GONE);
            djNameText.setVisibility(View.GONE);
            button1.setEnabled(false);
            button2.setEnabled(false);
            button3.setEnabled(false);
        }
    }
}
