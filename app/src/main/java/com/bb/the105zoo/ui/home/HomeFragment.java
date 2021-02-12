package com.bb.the105zoo.ui.home;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bb.the105zoo.R;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private HomeViewModel homeViewModel;
    Button button1;
    Button button2;
    Button button3;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Fragment locked in portrait screen orientation
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        button1 = root.findViewById(R.id.button1);
        button2 = root.findViewById(R.id.button2);
        button3 = root.findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);

        return root;
    }

    public void onClick(View target) {
        // Send the correct intent to the MusicService, according to the button that was clicked
        if (target == button1) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.PLAY");
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
        }
        else if (target == button2) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.PAUSE");
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
        }
        else if (target == button3) {
            Intent mIntent = new Intent();
            mIntent.setAction("com.bb.the105zoo.action.STOP");
            mIntent.setPackage(requireContext().getPackageName());
            requireContext().startService(mIntent);
        }
    }
}
