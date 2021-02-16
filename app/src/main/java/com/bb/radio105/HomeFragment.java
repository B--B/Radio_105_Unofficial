package com.bb.radio105;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment implements View.OnClickListener {

    Button button1;
    Button button2;
    Button button3;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        boolean service = isRadioStreamingRunning();
        boolean serviceInForeground = isRadioStreamingRunningInForeground();
        if (service) {
            if (!serviceInForeground) {
                // Pause state
                button1.setEnabled(true);
                button2.setEnabled(false);
            } else {
                // Playing state
                button1.setEnabled(false);
                button2.setEnabled(true);
            }
            button3.setEnabled(true);
        } else {
            // Stop state
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
        }

        IntentFilter receiverFilter1 = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        HeadsetIntentReceiver receiver1 = new HeadsetIntentReceiver();
        getContext().registerReceiver(receiver1, receiverFilter1);

        IntentFilter receiverFilter2 = new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        HeadsetIntentReceiver receiver2 = new HeadsetIntentReceiver();
        getContext().registerReceiver(receiver2, receiverFilter2);

        return root;
    }

    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == button1) {
            radioPlay(getActivity());
        }
        else if (target == button2) {
            radioPause(getActivity());
        }
        else if (target == button3) {
            radioStop(getActivity());
        }
    }

    private void radioPlay(Activity context) {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PLAY");
        mIntent.setPackage(context.getPackageName());
        context.startService(mIntent);
        button1.setEnabled(false);
        button2.setEnabled(true);
        button3.setEnabled(true);
    }

    private void radioPause(Activity context) {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PAUSE");
        mIntent.setPackage(context.getPackageName());
        context.startService(mIntent);
        button1.setEnabled(true);
        button2.setEnabled(false);
        button3.setEnabled(true);
    }

    private void radioStop(Activity activity) {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.STOP");
        mIntent.setPackage(activity.getPackageName());
        activity.startService(mIntent);
        button1.setEnabled(true);
        button2.setEnabled(false);
        button3.setEnabled(false);
    }

    private boolean isRadioStreamingRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isRadioStreamingRunningInForeground() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
