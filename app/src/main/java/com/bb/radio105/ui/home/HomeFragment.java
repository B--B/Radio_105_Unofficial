package com.bb.radio105.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bb.radio105.R;

public class HomeFragment extends Fragment implements View.OnClickListener {

    Button button1;
    Button button2;
    Button button3;
    private final String STATUS_KEY = "STATUS_KEY";
    private final String BUTTON1_ENABLED = "button1_selected";
    private final String BUTTON2_ENABLED = "button2_selected";
    private final String BUTTON3_ENABLED = "button3_selected";
    public static Boolean button1enabled;
    public static Boolean button2enabled;
    public static Boolean button3enabled;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        if (savedInstanceState != null) {
            boolean isButton1Enabled = savedInstanceState.getBoolean(BUTTON1_ENABLED);
            boolean isButton2Enabled = savedInstanceState.getBoolean(BUTTON2_ENABLED);
            boolean isButton3Enabled = savedInstanceState.getBoolean(BUTTON3_ENABLED);
            button1.setEnabled(isButton1Enabled);
            button2.setEnabled(isButton2Enabled);
            button3.setEnabled(isButton3Enabled);
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
        button1enabled = button1.isEnabled();
        button2enabled = button2.isEnabled();
        button3enabled = button3.isEnabled();
        outState.putBoolean(BUTTON1_ENABLED, button1.isEnabled());
        outState.putBoolean(BUTTON2_ENABLED, button2.isEnabled());
        outState.putBoolean(BUTTON3_ENABLED, button3.isEnabled());
        super.onSaveInstanceState(outState);
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
