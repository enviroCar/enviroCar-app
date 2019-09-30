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
package org.envirocar.app.views.preferences;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.views.preferences.bluetooth.SelectBluetoothAdapter;
import org.envirocar.app.handler.PreferenceConstants;

import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class SelectBluetoothPreference extends DialogPreference {

    // Injected variables.
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    protected ListView mBluetoothListView;

    private BluetoothDevice mCurrentlySelectedOBDDevice;

    /**
     * Constructor.
     *
     * @param context the Context of the current scope.
     * @param attrs   the attribute set.
     */
    public SelectBluetoothPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inject the required dependencies.
        BaseApplication.get(context).getBaseApplicationComponent().inject(this);

        // Set the layout resource.
        setDialogLayoutResource(R.layout.bluetooth_selection_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Find the list view for the bluetooth selection.
        mBluetoothListView = view.findViewById(R.id
                .bluetooth_selection_preference_list);

        Set<BluetoothDevice> pairedDevices = mBluetoothHandler.getPairedBluetoothDevices();
        ArrayList<String> names = new ArrayList<String>();
        for (BluetoothDevice device : pairedDevices) {
            names.add(device.getName());
        }

        SelectBluetoothAdapter adapter = new SelectBluetoothAdapter(getContext(), R.layout
                .bluetooth_selection_preference_list_entry,
                pairedDevices.toArray(new BluetoothDevice[pairedDevices.size()]));

        // Set the adapter and check the initial value as
        mBluetoothListView.setAdapter(adapter);

        if (mCurrentlySelectedOBDDevice != null) {
            // Set the initial value as checked.
            int position = adapter.getPosition(mCurrentlySelectedOBDDevice);
            if (position != -1) {
                mBluetoothListView.setItemChecked(position, true);
            }
        }

    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        if (restorePersistedValue) {
            String deviceAddress = getPersistedString("");
            if (!deviceAddress.isEmpty()) {
                mCurrentlySelectedOBDDevice = mBluetoothHandler.getBluetoothDeviceByAddress
                        (deviceAddress);

                // If there is a device already selected, then it updates the summary of the OBD
                // selection.
                if (mCurrentlySelectedOBDDevice != null)
                    setSummary(mCurrentlySelectedOBDDevice.getName() + " (" +
                            mCurrentlySelectedOBDDevice.getAddress() + ")");
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int position = mBluetoothListView.getCheckedItemPosition();
            BluetoothDevice device = (BluetoothDevice) mBluetoothListView.getItemAtPosition
                    (position);

            mCurrentlySelectedOBDDevice = device;
            if (device != null) {
                // Persist the new selected value and update the summery of the preference entry.
                setSummary(device.getName() + " (" + device.getAddress() + ")");
                persistString(device.getAddress());

                // Update the shared preference entry for the bluetooth selection tag.
                getSharedPreferences().edit()
                        .putString(PreferenceConstants.PREF_BLUETOOTH_NAME, device.getName())
                        .putString(PreferenceConstants.PREF_BLUETOOTH_ADDRESS, device
                                .getAddress())
                        .commit();
            }
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Remove default preference title.
        builder.setTitle(null);
    }

}
