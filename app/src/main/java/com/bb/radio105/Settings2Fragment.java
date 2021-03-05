package com.bb.radio105;

import android.content.SharedPreferences;
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
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Objects;

public class Settings2Fragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.SummaryProvider<androidx.preference.ListPreference> {

    View root;
    SettingsFragment mSettingsFragment;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_settings, container, false);

        // Stock Colors
        MainActivity.updateColorsInterface.onUpdate(false);

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

        private AlertDialog dialog;
        private ImageView mImageView;
        private Boolean isAlertDialogShowing = false;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
        @Override
        public boolean onPreferenceTreeClick(Preference p) {
            if (p.getKey().equals("thanks")) {
                showDialog();
                isAlertDialogShowing = true;
            }
            return false;
        }

        @Override
        public void onPause() {
            // Avoid a case where app is in background with alertDialog open
            // and image change causing a memory leak
            if (dialog != null) {
                mImageView.setImageDrawable(null);
                mImageView = null;
                dialog.dismiss();
                dialog = null;
            }
            super.onPause();
        }

        @Override
        public void onResume() {
            // Avoid a case where app is in background with alertDialog open
            // and image change causing a memory leak
            if (isAlertDialogShowing) {
                showDialog();
            }
            super.onResume();
        }

        @Override
        public void onDestroyView() {
            if (dialog != null) {
                mImageView.setImageDrawable(null);
                mImageView = null;
                dialog.dismiss();
                dialog = null;
            }
            super.onDestroyView();
        }

        void showDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.special_thanks, null, false);
            builder.setView(dialogView).
                    setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        dialog.cancel();
                        isAlertDialogShowing = false;
                    });
            dialog = builder.create();
            dialog.setIcon(R.drawable.ic_radio_105_logo);
            dialog.setTitle(R.string.special_thanks_title);
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
            dialog.show();
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.stack))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.google))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.unitedradio))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.mediasetplay))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.icons))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.jhey))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.adblockplus))).setMovementMethod(LinkMovementMethod.getInstance());
            ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.tinyPng))).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String darkModeString = getString(R.string.theme_key);
        if (key != null && sharedPreferences != null)
            if (key.equals(darkModeString)) {
                final String[] darkModeValues = getResources().getStringArray(R.array.theme_values);
                // The apps theme is decided depending upon the saved preferences on app startup
                String pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString(getString(R.string.theme_key), getString(R.string.theme_default_value));
                // Comparing to see which preference is selected and applying those theme settings
                if (pref.equals(darkModeValues[0]))
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                if (pref.equals(darkModeValues[1]))
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                if (pref.equals(darkModeValues[2]))
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
