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

import android.Manifest;
import android.app.UiModeManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Calendar;
import java.util.Objects;

import static android.content.Context.UI_MODE_SERVICE;

import com.google.android.material.snackbar.Snackbar;

public class Settings2Fragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.SummaryProvider<androidx.preference.ListPreference> {

    private View root;
    private SettingsFragment mSettingsFragment;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_settings, container, false);

        mSettingsFragment = new SettingsFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, mSettingsFragment)
                .commit();

        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);

        return root;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private AlertDialog thanksDialog;
        private AlertDialog miUiEMUIDialog;
        private ImageView mImageView;
        private Boolean isThanksDialogShowing = false;
        private Boolean isMiUiEMUIDialogShowing = false;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Preference Screen
            PreferenceScreen mPreferenceScreen = findPreference(getString(R.string.preference_key));
            // Preference Categories
            PreferenceCategory appNotificationPref = findPreference(getString(R.string.app_pref_key));
            PreferenceCategory streamingPref = findPreference(getString(R.string.streaming_pref_key));
            PreferenceCategory miUiEMUIPref = findPreference(getString(R.string.miui_emui_pref_key));
            PreferenceCategory webViewPref = findPreference(getString(R.string.webviews_pref_key));
            PreferenceCategory experimentalPref = findPreference(getString(R.string.experimental_key));
            // Preferences
            SwitchPreferenceCompat mediaNotification = findPreference(getString(R.string.notification_type_key));
            SwitchPreferenceCompat serviceKill = findPreference(getString(R.string.service_kill_key));
            SwitchPreferenceCompat webViewScreenOn = findPreference(getString(R.string.screen_on_key));

            // Android TV
            UiModeManager uiModeManager = (UiModeManager) requireActivity().getSystemService(UI_MODE_SERVICE);
            if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                if (webViewPref != null) {
                    assert webViewScreenOn != null;
                    webViewPref.removePreference(webViewScreenOn);
                }
                if (appNotificationPref != null) {
                    assert mediaNotification != null;
                    appNotificationPref.removePreference(mediaNotification);
                }
                if (streamingPref != null) {
                    assert serviceKill != null;
                    streamingPref.removePreference(serviceKill);
                }
            }
            // MiUi - EMUI
            if (Utils.isEMUI() || Utils.isMiUi()) {
                if (streamingPref != null) {
                    assert serviceKill != null;
                    streamingPref.removePreference(serviceKill);
                }
            } else {
                // Remove dialog for non MiUi/EMUI devices
                if (mPreferenceScreen != null) {
                    assert miUiEMUIPref != null;
                    mPreferenceScreen.removePreference(miUiEMUIPref);
                }
            }
            // Experimental Options
            boolean experimentalOptions = Utils.getUserPreferenceBoolean(requireContext(), getString(R.string.experimental_options_key), false);
            if (!experimentalOptions) {
                if (mPreferenceScreen != null) {
                    assert experimentalPref != null;
                    mPreferenceScreen.removePreference(experimentalPref);
                }
            }
            // Notification type listener for Android 13

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (appNotificationPref != null) {
                    if (mediaNotification != null) {
                        mediaNotification.setOnPreferenceChangeListener((preference, newValue) -> {
                            if (newValue.equals(false)) {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                                        Manifest.permission.POST_NOTIFICATIONS)) {
                                    Snackbar.make(requireView(), R.string.post_notification_required,
                                            Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, view -> {
                                        // Request the permission
                                        ActivityCompat.requestPermissions(requireActivity(),
                                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                                Constants.PERMISSION_REQUEST_POST_NOTIFICATIONS);
                                    }).show();
                                } else {
                                    // Request the permission. The result will be received in onRequestPermissionResult().
                                    ActivityCompat.requestPermissions(requireActivity(),
                                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, Constants.PERMISSION_REQUEST_POST_NOTIFICATIONS);
                                }
                            }
                            return true;
                        });
                    }
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference p) {
            if (p.getKey().equals(getString(R.string.thanks_key))) {
                showThanksDialog();
                isThanksDialogShowing = true;
            } else if (p.getKey().equals(getString(R.string.miui_emui_dialog_key))) {
                showMiUiEMUIDialog();
                isMiUiEMUIDialogShowing = true;
            }
            return false;
        }

        @Override
        public void onPause() {
            // Avoid a case where app is in background with alertDialog open
            // and image change causing a memory leak
            if (thanksDialog != null) {
                mImageView.setImageDrawable(null);
                mImageView = null;
                thanksDialog.dismiss();
                thanksDialog = null;
            } else if (miUiEMUIDialog != null) {
                miUiEMUIDialog.dismiss();
                miUiEMUIDialog = null;
            }
            super.onPause();
        }

        @Override
        public void onResume() {
            // Avoid a case where app is in background with alertDialog open
            // and image change causing a memory leak
            if (isThanksDialogShowing) {
                showThanksDialog();
            } else if (isMiUiEMUIDialogShowing) {
                showMiUiEMUIDialog();
            }
            super.onResume();
        }

        @Override
        public void onDestroyView() {
            if (thanksDialog != null) {
                mImageView.setImageDrawable(null);
                mImageView = null;
                thanksDialog.dismiss();
                thanksDialog = null;
            } else if (miUiEMUIDialog != null) {
                miUiEMUIDialog.dismiss();
                miUiEMUIDialog = null;
            }
            super.onDestroyView();
        }

        private void showThanksDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.special_thanks, null, false);
            builder.setView(dialogView).
                    setOnCancelListener(dialog -> isThanksDialogShowing = false);
            builder.setView(dialogView).
                    setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.cancel());
            thanksDialog = builder.create();
            thanksDialog.setIcon(R.drawable.ic_radio_105_logo);
            thanksDialog.setTitle(R.string.special_thanks_title);
            mImageView = dialogView.findViewById(R.id.ee);
            Calendar mCalendar = Calendar.getInstance();
            int timeOfDay = mCalendar.get(Calendar.HOUR_OF_DAY);
            if (timeOfDay >=7 && timeOfDay <11) {
                mImageView.setImageResource(R.drawable.easter_egg_1);
            } else if (timeOfDay >= 11 && timeOfDay < 14) {
                mImageView.setImageResource(R.drawable.easter_egg_2);
            } else if (timeOfDay >= 14 && timeOfDay < 19) {
                mImageView.setImageResource(R.drawable.easter_egg_3);
            } else if (timeOfDay >= 19 && timeOfDay < 23) {
                mImageView.setImageResource(R.drawable.easter_egg_4);
            } else if (timeOfDay == 23) {
                mImageView.setImageResource(R.drawable.easter_egg_5);
            } else if (timeOfDay < 3) {
                mImageView.setImageResource(R.drawable.easter_egg_5);
            } else {
                mImageView.setImageResource(R.drawable.easter_egg_6);
            }
            thanksDialog.show();
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.stack))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.google))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.unitedradio))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.mediasetplay))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.icons))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.jhey))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.adblockplus))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.tinyPng))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(thanksDialog.findViewById(R.id.social))).setMovementMethod(LinkMovementMethod.getInstance());
        }

        private void showMiUiEMUIDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.miui_emui_devices, null, false);
            builder.setView(dialogView).
                    setOnCancelListener(dialog -> isMiUiEMUIDialogShowing = false);
            builder.setView(dialogView).
                    setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.cancel());
            miUiEMUIDialog = builder.create();
            miUiEMUIDialog.setIcon(R.drawable.ic_radio_105_logo);
            miUiEMUIDialog.setTitle(R.string.miui_emui_dialog);
            miUiEMUIDialog.show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String darkModeString = getString(R.string.theme_key);
        if (key != null && sharedPreferences != null)
            if (key.equals(darkModeString)) {
                final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
                // The apps theme is decided depending upon the saved preferences on app startup
                String darkMode = Utils.getUserPreferenceString(requireContext(), getString(R.string.theme_key), getString(R.string.theme_default_value));
                // Comparing to see which preference is selected and applying those theme settings
                if (darkMode.equals(darkModeValues[0]))
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                if (darkMode.equals(darkModeValues[1]))
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                if (darkMode.equals(darkModeValues[2]))
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
    }

    @Override
    public CharSequence provideSummary(ListPreference preference) {
        String key = preference.getKey();
        if (key != null)
            if (key.equals(getString(R.string.theme_key)))
                return preference.getEntry();
        return null;
    }

    @Override
    public void onDestroyView() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .remove(mSettingsFragment)
                .commitAllowingStateLoss();
        root = null;
        super.onDestroyView();
    }
}
