package org.envirocar.app.view.preferences;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.view.preferences.bluetooth.SelectBluetoothAdapter;
import org.envirocar.core.injection.Injector;
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
        ((Injector) context.getApplicationContext()).injectObjects(this);

        // Set the layout resource.
        setDialogLayoutResource(R.layout.bluetooth_selection_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Find the list view for the bluetooth selection.
        mBluetoothListView = (ListView) view.findViewById(R.id
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
