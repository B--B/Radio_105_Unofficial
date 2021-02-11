package com.bb.the105zoo.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
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

import com.bb.the105zoo.MusicService;
import com.bb.the105zoo.R;

import java.io.IOException;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private HomeViewModel homeViewModel;
    Button button1;
    Button button2;
    Button button3;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
//        final TextView textView = root.findViewById(R.id.text_home);
//        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        return root;
    }

    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == button1) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.PLAY");
            mIntent.setPackage(getContext().getPackageName());
            getContext().startService(mIntent);
        }
        else if (target == button2) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.PAUSE");
            mIntent.setPackage(getContext().getPackageName());
            getContext().startService(mIntent);
        }
        else if (target == button3) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.STOP");
            mIntent.setPackage(getContext().getPackageName());
            getContext().startService(mIntent);
        }
    }
}
