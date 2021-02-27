package com.bb.radio105;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class Settings2Fragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.SummaryProvider<androidx.preference.ListPreference> {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        if (savedInstanceState == null) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new Settings2Fragment.SettingsFragment())
                    .commit();
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);

        return root;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
        @Override
        public boolean onPreferenceTreeClick(Preference p) {
            if (p.getKey().equals("thanks")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.special_thanks, null);
                builder.setView(dialogView).
                        setPositiveButton("Ok", (dialog, which) -> {
//                                Ok
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.stack))).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.google))).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.unitedradio))).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.mediasetplay))).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.icons))).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.jhey))).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) Objects.requireNonNull(dialog.findViewById(R.id.adblockplus))).setMovementMethod(LinkMovementMethod.getInstance());
            }
            return false;
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
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
