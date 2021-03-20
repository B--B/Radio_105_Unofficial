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
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import static com.bb.radio105.MusicService.mState;

public class HomeFragment extends Fragment {

    private Button button1;
    private Button button2;
    private Button button3;
    private View root;
    private MediaBrowserCompat mediaBrowser;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        root = inflater.inflate(R.layout.fragment_home, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().moveTaskToBack(true);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        // Create MediaBrowserServiceCompat
        mediaBrowser = new MediaBrowserCompat(getContext(),
                new ComponentName(getContext(), MusicService.class),
                connectionCallbacks,
                null); // optional Bundle

        // Set buttons state
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            button1.setEnabled(false);
            button2.setEnabled(true);
            button3.setEnabled(true);
        } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(true);
        } else if (mState == PlaybackStateCompat.STATE_STOPPED) {
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        } else if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            button1.setEnabled(false);
            button2.setEnabled(false);
            button3.setEnabled(false);
        }

        return root;
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        getParentFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onStart() {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(requireActivity()) != null) {
            MediaControllerCompat.getMediaController(requireActivity()).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    @Override
    public void onDestroyView() {
        button1 = null;
        button2 = null;
        button3 = null;
        root = null;
        super.onDestroyView();
    }

    void buildTransportControls() {

        // Attach a listener to the button
        button1.setOnClickListener(v -> MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().play());
        button2.setOnClickListener(v -> MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().pause());
        button3.setOnClickListener(v -> MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().stop());

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(requireActivity());

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback);

    }

    // ********* MediaBrowserCompat.ConnectionCallback implementation:
    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {

                    // Get the token for the MediaSession
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                    // Create a MediaControllerCompat
                    MediaControllerCompat mediaController =
                            new MediaControllerCompat(getContext(), // Context
                                    token);

                    // Save the controller
                    MediaControllerCompat.setMediaController(requireActivity(), mediaController);

                    // Finish building the UI
                    buildTransportControls();
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }
            };

    final MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                        button1.setEnabled(false);
                        button2.setEnabled(true);
                        button3.setEnabled(true);
                    } else if (state.getState() == PlaybackStateCompat.STATE_PAUSED) {
                        button1.setEnabled(true);
                        button2.setEnabled(false);
                        button3.setEnabled(true);
                    } else if (state.getState() == PlaybackStateCompat.STATE_STOPPED) {
                        button1.setEnabled(true);
                        button2.setEnabled(false);
                        button3.setEnabled(false);
                    } else if (state.getState() == PlaybackStateCompat.STATE_BUFFERING) {
                        button1.setEnabled(false);
                        button2.setEnabled(false);
                        button3.setEnabled(false);
                    }
                }
            };
}
