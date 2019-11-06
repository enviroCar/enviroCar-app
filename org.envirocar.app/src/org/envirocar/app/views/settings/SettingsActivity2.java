package org.envirocar.app.views.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.envirocar.app.R;
import org.envirocar.app.views.settings.custom.AutoConnectIntervalPreference;
import org.envirocar.app.views.settings.custom.GPSTrimDurationPreference;
import org.envirocar.app.views.settings.custom.SamplingRatePreference;
import org.envirocar.app.views.settings.custom.TimePickerPreferenceDialog;

/**
 * @author dewall
 */
public class SettingsActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // add the settingsfragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_settings_content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private Preference automaticRecording;
        private Preference searchInterval;
        private Preference enableGPSMode;
        private Preference gpsTrimDuration;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // find all preferences
            this.automaticRecording = findPreference(getString(R.string.prefkey_automatic_recording));
            this.searchInterval = findPreference(getString(R.string.prefkey_search_interval));
            this.enableGPSMode = findPreference(getString(R.string.prefkey_enable_gps_based_track_recording));
            this.gpsTrimDuration = findPreference(getString(R.string.prefkey_track_cut_duration));

            // set initial state
            this.searchInterval.setEnabled(((CheckBoxPreference) automaticRecording).isChecked());
            this.gpsTrimDuration.setVisible(((CheckBoxPreference) enableGPSMode).isChecked());

            // set preference change listener
            this.automaticRecording.setOnPreferenceChangeListener((preference, newValue) -> {
                searchInterval.setEnabled((boolean) newValue);
                return true;
            });
            this.enableGPSMode.setOnPreferenceChangeListener(((preference, newValue) -> {
                gpsTrimDuration.setVisible((boolean) newValue);
                return true;
            }));
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment fragment = null;
            if (preference instanceof AutoConnectIntervalPreference) {
                fragment = TimePickerPreferenceDialog.newInstance(preference.getKey());
            } else if (preference instanceof SamplingRatePreference) {
                fragment = SamplingRatePreference.Dialog.newInstance(preference.getKey());
            } else if (preference instanceof GPSTrimDurationPreference) {
                fragment = TimePickerPreferenceDialog.newInstance(preference.getKey());
            }

            if (fragment != null) {
                fragment.setTargetFragment(this, 0);
                fragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}
