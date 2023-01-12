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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bb.radio105.databinding.FragmentRadioBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import timber.log.Timber;

public class RadioFragment extends Fragment {

    private View root;
    private RadioServiceBinder mRadioServiceBinder;
    private MediaControllerCompat mMediaControllerCompat;
    boolean mBound = false;
    private Intent startMusicService;
    private FragmentRadioBinding mFragmentRadioBinding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mFragmentRadioBinding = FragmentRadioBinding.inflate(inflater, container, false);
        root = mFragmentRadioBinding.getRoot();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().moveTaskToBack(true);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        mFragmentRadioBinding.button1.setEnabled(false);
        mFragmentRadioBinding.button2.setEnabled(false);
        mFragmentRadioBinding.button3.setEnabled(false);

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
    }

    @Override
    public void onResume() {
        super.onResume();
        requireContext().bindService(new Intent(getContext(), RadioService.class), mServiceConnection, 0);
        buildTransportControls();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unbindService(mServiceConnection);
        mRadioServiceBinder = null;
    }

    @Override
    public void onStop() {
        super.onStop();
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
        mFragmentRadioBinding = null;
        root = null;
    }

    void buildTransportControls() {
        // Attach a listener to the button
        mFragmentRadioBinding.button1.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().play();
                Utils.fadeOutImageView(mFragmentRadioBinding.imageLogo);
            }
        });
        mFragmentRadioBinding.button2.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().pause();
                Utils.fadeOutImageView(mFragmentRadioBinding.imageArt);
                Utils.fadeInImageView(mFragmentRadioBinding.imageLogo, 250);
                Utils.fadeOutTextView(mFragmentRadioBinding.titleText);
                Utils.fadeOutTextView(mFragmentRadioBinding.djNameText);
            }
        });
        mFragmentRadioBinding.button3.setOnClickListener(v -> {
            if (mBound) {
                mMediaControllerCompat.getTransportControls().stop();
                if (mFragmentRadioBinding.imageArt.getVisibility() == View.VISIBLE) {
                    Utils.fadeOutImageView(mFragmentRadioBinding.imageArt);
                    Utils.fadeOutTextView(mFragmentRadioBinding.titleText);
                    Utils.fadeOutTextView(mFragmentRadioBinding.djNameText);
                    Utils.fadeInImageView(mFragmentRadioBinding.imageLogo, 250);
                } else if (mFragmentRadioBinding.imageArt.getVisibility() != View.VISIBLE && mFragmentRadioBinding.imageLogo.getVisibility() != View.VISIBLE) {
                    Utils.fadeInImageView(mFragmentRadioBinding.imageLogo, 250);
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
                mFragmentRadioBinding.imageArt.setImageDrawable(imageResource);
                mFragmentRadioBinding.titleText.setText(mRadioServiceBinder.getTitleString());
                mFragmentRadioBinding.djNameText.setText(mRadioServiceBinder.getDjString());
                mFragmentRadioBinding.imageArt.setVisibility(View.VISIBLE);
                mFragmentRadioBinding.imageLogo.setVisibility(View.INVISIBLE);
                mFragmentRadioBinding.titleText.setVisibility(View.VISIBLE);
                mFragmentRadioBinding.djNameText.setVisibility(View.VISIBLE);
            } else {
                mFragmentRadioBinding.imageArt.setVisibility(View.INVISIBLE);
                mFragmentRadioBinding.imageLogo.setVisibility(View.VISIBLE);
                mFragmentRadioBinding.titleText.setVisibility(View.INVISIBLE);
                mFragmentRadioBinding.djNameText.setVisibility(View.INVISIBLE);
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
                if (!Objects.equals(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE), mFragmentRadioBinding.titleText.getText().toString())) {
                    Timber.e("Title changed, update metadata. Old title: %s%s%s", mFragmentRadioBinding.titleText.getText().toString(), " new title: ", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                    Drawable imageResource = new BitmapDrawable(getResources(), metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
                    mFragmentRadioBinding.imageArt.setImageDrawable(imageResource);
                    mFragmentRadioBinding.titleText.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                    mFragmentRadioBinding.djNameText.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
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
                    if (mFragmentRadioBinding.imageLogo.getVisibility() != View.INVISIBLE || mFragmentRadioBinding.imageArt.getVisibility() != View.VISIBLE) {
                        if (RadioService.fromPauseState) {
                            Utils.fadeInImageView(mFragmentRadioBinding.imageArt, 250);
                            Utils.fadeInTextView(mFragmentRadioBinding.titleText, 250);
                            Utils.fadeInTextView(mFragmentRadioBinding.djNameText, 250);
                        } else {
                            Utils.fadeInImageView(mFragmentRadioBinding.imageArt, 0);
                            Utils.fadeInTextView(mFragmentRadioBinding.titleText, 0);
                            Utils.fadeInTextView(mFragmentRadioBinding.djNameText, 0);
                        }
                    }
                    mFragmentRadioBinding.button1.setEnabled(false);
                    mFragmentRadioBinding.button2.setEnabled(true);
                    mFragmentRadioBinding.button3.setEnabled(true);
                    break;
                case STATE_PAUSED:
                    mFragmentRadioBinding.button1.setEnabled(true);
                    mFragmentRadioBinding.button2.setEnabled(false);
                    mFragmentRadioBinding.button3.setEnabled(true);
                    break;
                case STATE_BUFFERING:
                    mFragmentRadioBinding.button1.setEnabled(false);
                    mFragmentRadioBinding.button2.setEnabled(false);
                    mFragmentRadioBinding.button3.setEnabled(true);
                    break;
                case STATE_STOPPED:
                    mFragmentRadioBinding.button1.setEnabled(true);
                    mFragmentRadioBinding.button2.setEnabled(false);
                    mFragmentRadioBinding.button3.setEnabled(false);
                    break;
                case STATE_ERROR:
                    if (mFragmentRadioBinding.imageLogo.getVisibility() != View.VISIBLE) {
                        Utils.fadeOutImageView(mFragmentRadioBinding.imageArt);
                        Utils.fadeInImageView(mFragmentRadioBinding.imageLogo, 250);
                        Utils.fadeOutTextView(mFragmentRadioBinding.titleText);
                        Utils.fadeOutTextView(mFragmentRadioBinding.djNameText);
                    }
                    mFragmentRadioBinding.button1.setEnabled(true);
                    mFragmentRadioBinding.button2.setEnabled(false);
                    mFragmentRadioBinding.button3.setEnabled(false);
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
