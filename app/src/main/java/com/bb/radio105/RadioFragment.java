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

import static android.support.v4.media.session.PlaybackStateCompat.*;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

import java.util.Objects;

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
        super.onStart();
        startMusicService = new Intent(requireContext(), RadioService.class);
        requireContext().startService(startMusicService);
        requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
        buildTransportControls();
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unbindService(mServiceConnection);
        mRadioServiceBinder = null;
        if (RadioService.mState == STATE_STOPPED) {
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
                Utils.fadeOutImageView(imageLogo);
            }
        });
        button2.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().pause();
                Utils.fadeOutImageView(imageArt);
                Utils.fadeInImageView(imageLogo, 250);
                Utils.fadeOutTextView(titleText);
                Utils.fadeOutTextView(djNameText);
            }
        });
        button3.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().stop();
                if (imageArt.getVisibility() == View.VISIBLE) {
                    Utils.fadeOutImageView(imageArt);
                    Utils.fadeOutTextView(titleText);
                    Utils.fadeOutTextView(djNameText);
                    Utils.fadeInImageView(imageLogo, 250);
                } else if (imageArt.getVisibility() != View.VISIBLE && imageLogo.getVisibility() != View.VISIBLE) {
                    Utils.fadeInImageView(imageLogo, 250);
                }
            }
        });
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
            if (RadioService.mState == STATE_PLAYING) {
                Drawable imageResource = new BitmapDrawable(getResources(), mRadioServiceBinder.getArt());
                imageArt.setImageDrawable(imageResource);
                titleText.setText(mRadioServiceBinder.getTitleString());
                djNameText.setText(mRadioServiceBinder.getDjString());
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
            if (RadioService.mState == STATE_PLAYING) {
                Timber.e("Metadata changed during play state, check if the Radio fragment must be updated");
                if (!Objects.equals(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE), titleText.getText().toString())) {
                    Timber.e("Title changed, update metadata. Old title: %s%s%s", titleText.getText().toString(), " new title: ", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                    Drawable imageResource = new BitmapDrawable(getResources(), metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
                    imageArt.setImageDrawable(imageResource);
                    titleText.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                    djNameText.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
                }
            } else {
                Timber.e("Metadata changed, but we are not in play state");
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            Timber.e("Playback state changed, new state is: %s", state.getState());
            switch (state.getState()) {
                case STATE_PLAYING:
                    if (imageLogo.getVisibility() != View.INVISIBLE || imageArt.getVisibility() != View.VISIBLE) {
                        if (RadioService.fromPauseState) {
                            Utils.fadeInImageView(imageArt, 250);
                            Utils.fadeInTextView(titleText, 250);
                            Utils.fadeInTextView(djNameText, 250);
                        } else {
                            Utils.fadeInImageView(imageArt, 0);
                            Utils.fadeInTextView(titleText, 0);
                            Utils.fadeInTextView(djNameText, 0);
                        }
                    }
                    button1.setEnabled(false);
                    button2.setEnabled(true);
                    button3.setEnabled(true);
                    break;
                case STATE_PAUSED:
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(true);
                    break;
                case STATE_BUFFERING:
                    button1.setEnabled(false);
                    button2.setEnabled(false);
                    button3.setEnabled(true);
                    break;
                case STATE_STOPPED:
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(false);
                    break;
                case STATE_ERROR:
                    if (imageLogo.getVisibility() != View.VISIBLE) {
                        Utils.fadeOutImageView(imageArt);
                        Utils.fadeInImageView(imageLogo, 250);
                        Utils.fadeOutTextView(titleText);
                        Utils.fadeOutTextView(djNameText);
                    }
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(false);
                    break;
                case STATE_CONNECTING:
                case STATE_FAST_FORWARDING:
                case STATE_NONE:
                case STATE_REWINDING:
                case STATE_SKIPPING_TO_NEXT:
                case STATE_SKIPPING_TO_PREVIOUS:
                case STATE_SKIPPING_TO_QUEUE_ITEM:
                    break;
            }
        }
    };
}
