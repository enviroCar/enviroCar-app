/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
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
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.views.settings.custom.AutoConnectIntervalPreference;
import org.envirocar.app.views.settings.custom.GPSTrimDurationPreference;
import org.envirocar.app.views.settings.custom.SamplingRatePreference;
import org.envirocar.app.views.settings.custom.TimePickerPreferenceDialog;

/**
 * @author dewall
 */
public class SettingsActivity extends AppCompatActivity {

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

        private Preference automaticUpload;
        private Preference automaticRecording;
        private Preference searchInterval;
        private Preference enableGPSMode;
        private Preference gpsTrimDuration;
        private Preference gpsAutoRecording;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // find all preferences
            this.automaticUpload = findPreference(getString(R.string.prefkey_always_upload));
            this.automaticRecording = findPreference(getString(R.string.prefkey_automatic_recording));
            this.searchInterval = findPreference(getString(R.string.prefkey_search_interval));
            this.enableGPSMode = findPreference(getString(R.string.prefkey_enable_gps_based_track_recording));
            this.gpsTrimDuration = findPreference(getString(R.string.prefkey_track_trim_duration));
            this.gpsAutoRecording = findPreference(getString(R.string.prefkey_gps_mode_ar));

            // disable automatic upload for now.
            this.automaticUpload.setVisible(false);

            // set initial state
            this.searchInterval.setVisible(((CheckBoxPreference) automaticRecording).isChecked());
            this.gpsTrimDuration.setVisible(((CheckBoxPreference) enableGPSMode).isChecked());
            this.gpsAutoRecording.setVisible(((CheckBoxPreference) enableGPSMode).isChecked());

            // set preference change listener
            this.automaticRecording.setOnPreferenceChangeListener((preference, newValue) -> {
                searchInterval.setVisible((boolean) newValue);
                return true;
            });
            this.enableGPSMode.setOnPreferenceChangeListener(((preference, newValue) -> {
                if (!(boolean) newValue) {
                    ApplicationSettings.setSelectedRecordingType(getContext(), RecordingType.OBD_ADAPTER_BASED);
                }
                gpsTrimDuration.setVisible((boolean) newValue);
                gpsAutoRecording.setVisible((boolean) newValue);
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
