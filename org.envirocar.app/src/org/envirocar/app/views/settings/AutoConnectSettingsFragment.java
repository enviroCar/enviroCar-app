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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.services.AutomaticTrackRecordingService;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.utils.ServiceUtils;

import javax.inject.Inject;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class AutoConnectSettingsFragment extends PreferenceFragment {
    private static final Logger LOG = Logger.getLogger(AutoConnectSettingsFragment.class);

    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected RxSharedPreferences rxSharedPreferences;
    @InjectApplicationScope
    @Inject
    protected Context context;

    private CheckBoxPreference mBackgroundServicePreference;
    private CheckBoxPreference mAutoConnectPrefrence;
    private Preference mSearchIntervalPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inject all required dependencies.
        BaseApplication.get(getActivity()).getBaseApplicationComponent().inject(this);

        // Set the preference resource.
        addPreferencesFromResource(R.xml.preferences_auto_connect);

        // Get all preferences
        mBackgroundServicePreference = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(PreferenceConstants.PREF_BLUETOOTH_SERVICE_AUTOSTART);
        mAutoConnectPrefrence = (CheckBoxPreference)  getPreferenceScreen()
                .findPreference(PreferenceConstants.PREF_BLUETOOTH_AUTOCONNECT);
        mSearchIntervalPreference = getPreferenceScreen()
                .findPreference(PreferenceConstants.PREF_BLUETOOTH_DISCOVERY_INTERVAL);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // SwitchPreference preference change listener
        mBackgroundServicePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (boolean) newValue;
            mAutoConnectPrefrence.setEnabled(isChecked);
            if (isChecked) {
                mSearchIntervalPreference.setEnabled(mAutoConnectPrefrence.isChecked());
                if(!ServiceUtils.isServiceRunning(
                        context, AutomaticTrackRecordingService.class)) {
                    Intent startIntent = new Intent(context, AutomaticTrackRecordingService.class);
                    context.startService(startIntent);
                }
            } else {
                mAutoConnectPrefrence.setChecked(false);
                mSearchIntervalPreference.setEnabled(false);
            }
            return true;
        });

        mAutoConnectPrefrence.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (boolean) newValue;
            mSearchIntervalPreference.setEnabled(isChecked);
            return true;
        });

        if(!mBackgroundServicePreference.isChecked()){
            mAutoConnectPrefrence.setEnabled(false);
            mSearchIntervalPreference.setEnabled(false);
        }

        if(!mAutoConnectPrefrence.isChecked()){
            mSearchIntervalPreference.setEnabled(false);
        }

        // Set the color of the background from transparent to white.
        getView().setBackgroundColor(getResources().getColor(R.color.white_cario));
    }

    @Override
    public void onStart() {
        super.onStart();

        // Register this object on the event bus.
        mBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister this object from the event bus.
        mBus.unregister(this);
    }

}
