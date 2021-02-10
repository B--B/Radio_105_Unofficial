package com.bb.the105zoo.ui.home;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bb.the105zoo.R;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private static MediaPlayer mPlayer;
    private Button button1;
    private Button button2;
    private Button button3;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
        String url = "http://icy.unitedradio.it/Radio105.mp3"; // initialize Uri here
        mPlayer = new MediaPlayer();
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            AudioAttributes.Builder b = new AudioAttributes.Builder();
            b.setUsage(AudioAttributes.USAGE_MEDIA);
            mPlayer.setAudioAttributes(b.build());
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        try {
            mPlayer.setDataSource(url);
            mPlayer.prepareAsync();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    button1.setEnabled(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
//        final TextView textView = root.findViewById(R.id.text_home);
//        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        button1.setOnClickListener(v -> {
            if (mPlayer.isPlaying()) {
//                Do nothing
            } else {
                mPlayer.start();
                button1.setEnabled(false);
                button2.setEnabled(true);
            }
        });

        button2.setOnClickListener(v -> {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(true);
            } else {
//                Do nothing
            }
        });

        button3.setOnClickListener(v -> {
            mPlayer.reset();
            try {
                mPlayer.setDataSource(url);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mPlayer.start();
                        button1.setEnabled(false);
                        button2.setEnabled(true);
                        button3.setEnabled(false);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        return root;
    }
}
