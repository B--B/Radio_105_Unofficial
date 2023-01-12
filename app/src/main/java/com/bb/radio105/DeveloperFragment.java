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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.bb.radio105.databinding.FragmentDeveloperBinding;

public class DeveloperFragment extends Fragment {

    private static final String ARG_APPLICATION_ID = BuildConfig.APPLICATION_ID;

    private View root;

    private String versionNumber;
    private int clickCount = 0;
    private FragmentDeveloperBinding mFragmentDeveloperBinding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mFragmentDeveloperBinding = FragmentDeveloperBinding.inflate(inflater, container, false);
        root = mFragmentDeveloperBinding.getRoot();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                 Navigation.findNavController(root).navigate(R.id.nav_home);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        versionNumber = BuildConfig.VERSION_NAME;

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFragmentDeveloperBinding.sources.setOnClickListener(view1 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/B--B/Radio_105_Unofficial"))));

        mFragmentDeveloperBinding.bug.setOnClickListener(view2 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/B--B/Radio_105_Unofficial/issues"))));

        mFragmentDeveloperBinding.developerMail.setOnClickListener(view3 -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "mrczn.bb@gmail.com", null))
                    .putExtra(Intent.EXTRA_EMAIL, "mrczn.bb@gmail.com");
            startActivity(Intent.createChooser(emailIntent, view3.getContext().getString(R.string.send_email)));
        });

        mFragmentDeveloperBinding.ratePlayStore.setOnClickListener(view4 -> {
            // To count with Play market back stack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
            Intent goToMarket = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + ARG_APPLICATION_ID))
                    .addFlags(flags);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + ARG_APPLICATION_ID)));
            }
        });

        final String recommendSubject;
        recommendSubject = getString(R.string.get_the_app);

        mFragmentDeveloperBinding.recommendToFriend.setOnClickListener(view5 -> {
            String text = getString(R.string.get_the_app) + System.getProperty("line.separator") + Uri.parse("https://play.google.com/store/apps/details?id=" + (ARG_APPLICATION_ID)).toString();

            Intent sharingIntent = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, recommendSubject)
                    .putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(sharingIntent, view5.getContext().getString(R.string.share_via)));
        });

        final String appVersionString;
        appVersionString = getString(R.string.app_version) + versionNumber;
        mFragmentDeveloperBinding.appVersion.setText(appVersionString);
        mFragmentDeveloperBinding.appVersion.setOnClickListener(view6 -> {
            clickCount ++;
            if (clickCount == 7) {
                boolean experimentalOptions = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.experimental_options_key), false);
                Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                if (experimentalOptions) {
                    editor.putBoolean(getString(R.string.experimental_options_key), false);
                    editor.apply();
                    Toast.makeText(getActivity(), R.string.experimental_disabled,
                            Toast.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean(getString(R.string.experimental_options_key), true);
                    editor.apply();
                    Toast.makeText(getActivity(), R.string.experimental_enabled,
                            Toast.LENGTH_SHORT).show();
                }
                clickCount = 0;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFragmentDeveloperBinding = null;
        root = null;
    }
}
