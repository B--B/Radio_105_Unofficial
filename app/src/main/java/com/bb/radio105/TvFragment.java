package com.bb.radio105;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class TvFragment extends Fragment {

    private ProgressBar progressBar;
    VideoView videoView;
    String videoUrl;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tv, container, false);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = requireActivity().getWindow().getInsetsController();

                if (controller != null)
                    controller.hide(WindowInsets.Type.statusBars());
            } else {
                requireActivity().getWindow().addFlags(FLAG_FULLSCREEN);
            }
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).hide();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = requireActivity().getWindow().getInsetsController();

            if (controller != null)
                controller.show(WindowInsets.Type.statusBars());
        } else {
                requireActivity().getWindow().clearFlags(FLAG_FULLSCREEN);
            }
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).show();
        }

        videoView = root.findViewById(R.id.videoView);
        progressBar = root.findViewById(R.id.progressBar);
        videoUrl = "https://live2-radio-mediaset-it.akamaized.net/content/hls_h0_clr_vos/live/channel(ec)/index.m3u8";

        // Stop radio streaming if running
        boolean service = Utils.isRadioStreamingRunningInForeground(requireContext());
        if (service) {
             if (MusicService.mState == MusicService.State.Playing) {
                 Intent mIntent = new Intent();
                 mIntent.setAction(Constants.ACTION_STOP);
                 mIntent.setPackage(requireContext().getPackageName());
                 requireContext().startService(mIntent);
                 requireContext().sendBroadcast(mIntent);
             }
         }

        // Start video streaming
        progressBar.setVisibility(View.VISIBLE);
        videoView.requestFocus();
        videoView.setOnInfoListener(onInfoToPlayStateListener);
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.start();

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
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = requireActivity().getWindow().getInsetsController();

                if (controller != null)
                    controller.hide(WindowInsets.Type.statusBars());
            } else {
                requireActivity().getWindow().addFlags(FLAG_FULLSCREEN);
            }
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).hide();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = requireActivity().getWindow().getInsetsController();

                if (controller != null)
                    controller.show(WindowInsets.Type.statusBars());
            } else {
                requireActivity().getWindow().clearFlags(FLAG_FULLSCREEN);
            }
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = requireActivity().getWindow().getInsetsController();

                if (controller != null)
                    controller.show(WindowInsets.Type.statusBars());
            } else {
                requireActivity().getWindow().clearFlags(FLAG_FULLSCREEN);
            }
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).show();
        }
    }
}
