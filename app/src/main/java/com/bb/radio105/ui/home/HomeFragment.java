package com.bb.radio105.ui.home;

import android.app.Activity;
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
    public static String status;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        // If STATUS_KEY is null then we are starting the Activity for the first time
        if (savedInstanceState != null) {
            status = savedInstanceState.getString(STATUS_KEY);
            updateButtonsState();
        } else {
            updateButtonsService();
        }

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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        outState.putString(STATUS_KEY, status);
        super.onSaveInstanceState(outState);
    }

    private void updateButtonsService() {
        // If user close the app while MusicService is running we don't have a valid STATUS_KEY.
        // We must check if MusicService is running and set enabled buttons accordingly.
        boolean service = isRadioStreamingRunning();
        if (service) {
            // Radio service is running
            button1.setEnabled(false);
            button2.setEnabled(true);
            button3.setEnabled(true);
        }
    }

    void updateButtonsState() {
        switch (HomeFragment.status) {
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
                throw new IllegalStateException("Unexpected value: " + HomeFragment.status);
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
        status = "play";
    }

    private void radioPause(Activity context) {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.PAUSE");
        mIntent.setPackage(context.getPackageName());
        context.startService(mIntent);
        button1.setEnabled(true);
        button2.setEnabled(false);
        button3.setEnabled(true);
        status = "pause";
    }

    private void radioStop(Activity activity) {
        Intent mIntent = new Intent();
        mIntent.setAction("com.bb.radio105.action.STOP");
        mIntent.setPackage(activity.getPackageName());
        activity.startService(mIntent);
        button1.setEnabled(true);
        button2.setEnabled(false);
        button3.setEnabled(false);
        status = "stop";
    }

    boolean isRadioStreamingRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
