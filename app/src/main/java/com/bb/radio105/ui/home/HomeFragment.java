package com.bb.radio105.ui.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bb.radio105.MusicService;
import com.bb.radio105.R;

public class HomeFragment extends Fragment implements View.OnClickListener {

    Button button1;
    Button button2;
    Button button3;
    private final String STATUS_KEY = "STATUS_KEY";
    String status;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        boolean service = isRadioStreamingRunning();
        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        // If STATUS_KEY is null then we are starting the Activity for the first time
        if (status != null) {
            status = savedInstanceState.getString(STATUS_KEY);
            switch (status) {
                case "play":
                    button1.setEnabled(false);
                    button2.setEnabled(true);
                    button3.setEnabled(true);
                    break;
                case "pause":
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(true);
                    break;
                case "stop":
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(false);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + status);
            }
        } else {
            // If user close the app while MusicService is running we don't have a valid STATUS_KEY.
            // We must check if MusicService is running and set enabled buttons accordingly.
            // This is also needed after theme pref addition, as (savedInstanceState != null) no longer works
            if (service) {
                // Radio service is running
                button1.setEnabled(false);
                button2.setEnabled(true);
                button3.setEnabled(true);
            } else {
                // Radio service is not running
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(false);
            }
        }

        return root;
    }

    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == button1) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.PLAY");
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
            button1.setEnabled(false);
            button2.setEnabled(true);
            button3.setEnabled(true);
            status = "play";
        }
        else if (target == button2) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.PAUSE");
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(true);
            status = "pause";
        }
        else if (target == button3) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.STOP");
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
            button1.setEnabled(true);
            button2.setEnabled(false);
            button3.setEnabled(false);
            status = "stop";
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(STATUS_KEY, status);
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
}
