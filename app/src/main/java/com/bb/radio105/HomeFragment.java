package com.bb.radio105;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;

public class HomeFragment extends Fragment implements View.OnClickListener, PlayerStatusListener {

    Button button1;
    Button button2;
    Button button3;

    static PlayerStatusListener playerStatusListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        playerStatusListener = this;

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Constants.ACTION_PLAY);
        mIntentFilter.addAction(Constants.ACTION_PAUSE);
        mIntentFilter.addAction(Constants.ACTION_STOP);
        mIntentFilter.addAction(Constants.ACTION_PLAY_NOTIFICATION);
        mIntentFilter.addAction(Constants.ACTION_PAUSE_NOTIFICATION);
        mIntentFilter.addAction(Constants.ACTION_STOP_NOTIFICATION);
        mIntentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        PlayerIntentReceiver playerIntentReceiver = new PlayerIntentReceiver();
        requireContext().registerReceiver(playerIntentReceiver, mIntentFilter);

        return root;
    }

    @Override
    public void onButtonStatusChange(String status) {
        switch (status) {
            case "Play":
            case "Play_Notification":
                button1.setEnabled(false);
                button2.setEnabled(true);
                button3.setEnabled(true);
                break;
            case "Pause":
            case "Pause_Notification":
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(true);
                break;
            case "Stop":
            case "Stop_Notification":
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(false);
                break;
            case "Audio_Device_Disconnected":
                boolean pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean(requireContext().getString(R.string.noisy_key), true);
                if (pref) {
                    Intent mIntent = new Intent();
                    mIntent.setAction(Constants.ACTION_PAUSE);
                    mIntent.setPackage(requireContext().getPackageName());
                    requireContext().startService(mIntent);
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(true);
                }
                break;
        }
    }

    @Override
    public void onStart() {

        boolean service = Utils.isRadioStreamingRunning(requireContext());
        boolean serviceInForeground = Utils.isRadioStreamingRunningInForeground(requireContext());
        if (service) {
            if (!serviceInForeground) {
                // Pause state
                button1.setEnabled(true);
                button2.setEnabled(false);
            } else {
                if (MusicService.mState == MusicService.State.Paused) {
                    // Pause with persistent notification option enabled
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                } else {
                    // Playing state
                    button1.setEnabled(false);
                    button2.setEnabled(true);
                }
            }
            button3.setEnabled(true);
        } else {
            // Stop state
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        }
        super.onStart();
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

    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == button1) {
            radioPlay(requireActivity());
        }
        else if (target == button2) {
            radioPause(requireActivity());
        }
        else if (target == button3) {
            radioStop(requireActivity());
        }
    }

    private void radioPlay(Activity context) {
        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_PLAY);
        mIntent.setPackage(context.getPackageName());
        context.startService(mIntent);
    }

    private void radioPause(Activity context) {
        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_PAUSE);
        mIntent.setPackage(context.getPackageName());
        context.startService(mIntent);
    }

    private void radioStop(Activity activity) {
        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_STOP);
        mIntent.setPackage(activity.getPackageName());
        activity.startService(mIntent);
    }
}
