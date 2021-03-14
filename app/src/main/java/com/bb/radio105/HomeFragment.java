/*
 * Copyright (C) 2021 Zanin Marco (B--B)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

public class HomeFragment extends Fragment implements View.OnClickListener, PlayerStatusListener {

    private Button button1;
    private Button button2;
    private Button button3;
    private View root;

    static PlayerStatusListener playerStatusListener;
    private final PlayerIntentReceiver playerIntentReceiver = new PlayerIntentReceiver();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_home, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().moveTaskToBack(true);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

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
        mIntentFilter.addAction(Constants.ACTION_ERROR);
        requireContext().registerReceiver(playerIntentReceiver, mIntentFilter);

        return root;
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {

        switch (MusicService.mState) {
            case Playing:
                button1.setEnabled(false);
                button2.setEnabled(true);
                button3.setEnabled(true);
                break;
            case Paused:
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(true);
                break;
            case Stopped:
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(false);
                break;
//            case Preparing:
//                button1.setEnabled(false);
//                button2.setEnabled(false);
//                button3.setEnabled(false);
//                break;
        }
        super.onViewCreated(view, savedInstanceState);
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
            case "Error":
            case "Stop":
            case "Stop_Notification":
                button1.setEnabled(true);
                button2.setEnabled(false);
                button3.setEnabled(false);
                break;
        }
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

    @Override
    public void onDestroyView() {
        requireContext().unregisterReceiver(playerIntentReceiver);
        playerStatusListener = null;
        button1 = null;
        button2 = null;
        button3 = null;
        root = null;
        super.onDestroyView();
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

    private void radioPlay(Activity mActivity) {
        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_PLAY);
        mIntent.setPackage(mActivity.getPackageName());
        mActivity.startService(mIntent);
    }

    private void radioPause(Activity mActivity) {
        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_PAUSE);
        mIntent.setPackage(mActivity.getPackageName());
        mActivity.startService(mIntent);
    }

    private void radioStop(Activity mActivity) {
        Intent mIntent = new Intent();
        mIntent.setAction(Constants.ACTION_STOP);
        mIntent.setPackage(mActivity.getPackageName());
        mActivity.startService(mIntent);
    }
}
