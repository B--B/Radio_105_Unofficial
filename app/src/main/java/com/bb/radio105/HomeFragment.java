package com.bb.radio105;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment implements View.OnClickListener, UpdateHeadphoneStatusListener {

    Button button1;
    Button button2;
    Button button3;

    static UpdateHeadphoneStatusListener headphoneDisconnected;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        headphoneDisconnected = this;

        boolean service = Utils.isRadioStreamingRunning(requireContext());
        boolean serviceInForeground = Utils.isRadioStreamingRunningInForeground(requireContext());
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
        requireContext().registerReceiver(receiver1, receiverFilter1);

        IntentFilter receiverFilter2 = new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        HeadsetIntentReceiver receiver2 = new HeadsetIntentReceiver();
        requireContext().registerReceiver(receiver2, receiverFilter2);

        return root;
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

    @Override
    public void onUpdate (boolean disconnected) {
        if (disconnected){
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
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
}
