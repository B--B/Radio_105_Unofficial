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

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import static com.bb.radio105.Constants.ACTION_PAUSE;

public class TvFragment extends Fragment {

    private View root;
    private ProgressBar progressBar;
    private VideoView videoView;
    private String videoUrl;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_tv, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.setUpFullScreen(requireActivity());
        } else {
            Utils.restoreScreen(requireActivity());
        }

        // Stop radio streaming if running
        if (MusicService.mPlayer.isPlaying()) {
            Intent mIntent = new Intent();
            mIntent.setAction(ACTION_PAUSE);
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
        }

        return root;
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

    @Override
    public void onStart() {
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        videoView = root.findViewById(R.id.videoView);
        progressBar = root.findViewById(R.id.progressBar);
        videoUrl = "https://live2-radio-mediaset-it.akamaized.net/content/hls_h0_clr_vos/live/channel(ec)/index.m3u8";
        // Start video streaming
        progressBar.setVisibility(View.VISIBLE);
        videoView.requestFocus();
        videoView.setOnInfoListener(onInfoToPlayStateListener);
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.start();
        super.onStart();
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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.restoreScreen(requireActivity());
        }
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        videoView = null;
        progressBar = null;
        videoUrl = null;
        root = null;
        super.onDestroyView();
    }
}
