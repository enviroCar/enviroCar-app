/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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

import static org.envirocar.app.views.utils.SnackbarUtil.showGrantMicrophonePermission;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;

import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.recording.RecordingType;
import org.envirocar.app.views.modeldashboard.ModelDashboardActivity;
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

        private Preference automaticRecording;
        private Preference searchInterval;
        private Preference enableGPSMode;
        private Preference gpsTrimDuration;
        private Preference gpsAutoRecording;
        private Preference voiceModelDashboard;
        private CheckBoxPreference enableVoiceCommand;
        private SharedPreferences voiceModelPreferences;

        private final int RECORD_AUDIO_PERMISSION_REQ_CODE = 22;

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
            this.gpsTrimDuration = findPreference(getString(R.string.prefkey_track_trim_duration));
            this.gpsAutoRecording = findPreference(getString(R.string.prefkey_gps_mode_ar));
            this.enableVoiceCommand = findPreference(getString(R.string.prefkey_voice_command));
            this.voiceModelDashboard = findPreference("pref_model_dashboard");
            this.voiceModelPreferences = getActivity().getSharedPreferences("voicemodel", Context.MODE_PRIVATE);


            // set initial state
            this.searchInterval.setVisible(((CheckBoxPreference) automaticRecording).isChecked());
            this.gpsTrimDuration.setVisible(((CheckBoxPreference) enableGPSMode).isChecked());
            this.gpsAutoRecording.setVisible(((CheckBoxPreference) enableGPSMode).isChecked());
            this.voiceModelDashboard.setVisible(enableVoiceCommand.isChecked());
            this.voiceModelDashboard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), ModelDashboardActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

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

            this.enableVoiceCommand.setOnPreferenceChangeListener(((preference, newValue) -> {
                if ((boolean) newValue) {
                    requestMicrophonePermission();
                }
                else{
                    this.voiceModelDashboard.setVisible(false);
                }
                return true;
            }));
        }

        @Override
        public void onResume() {
            super.onResume();
            enableVoiceCommand.setChecked(voiceModelPreferences.getBoolean("prefkey_voice_model_downloaded",false));
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

        public void requestMicrophonePermission() {

            // if microphone permission is not granted, request it
            if (!(requireContext().checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED)) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
            else{
                onVoiceCommandEnabled();
            }
        }

        private ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        Snackbar.make(requireView(), R.string.microphone_permission_granted, Snackbar.LENGTH_LONG).show();
                        onVoiceCommandEnabled();
                    } else {

                        this.enableVoiceCommand.setChecked(false);
                        this.voiceModelDashboard.setVisible(false);
                        // action opens app's general settings where user can grant microphone/any permission
                        showGrantMicrophonePermission(requireView(), requireContext(), getActivity());
                    }
                });

        private void onVoiceCommandEnabled() {
            this.voiceModelDashboard.setVisible(true);
            Intent intent = new Intent(getActivity(), ModelDashboardActivity.class);
            startActivity(intent);
            enableVoiceCommand.setChecked(false);
        }

    }
}