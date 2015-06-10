package org.envirocar.app.activity.preference;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.envirocar.app.Injector;
import org.envirocar.app.R;
import org.envirocar.app.bluetooth.service.BluetoothHandler;

import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class SelectBluetoothPreference extends DialogPreference {

    // Injected variables.
    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    // Newly discovered devices
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayAdapter<String> mPairedDevicesAdapter;
    private ArrayList<BluetoothDevice> mNewBluetoothDevices;
    private ArrayList<BluetoothDevice> mPairedDevices;

    // Views for the already paired devices.
    private TextView mPairedDevicesTextView;
    private ListView mPairedDevicesListView;

    // Views for the newly discovered devices.
    private TextView mNewDevicesTextView;
    private ListView mNewDevicesListView;

    private ProgressBar mProgressBar;


    /**
     * Constructor.
     *
     * @param context the Context of the current scope.
     * @param attrs   the attribute set.
     */
    public SelectBluetoothPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        ((Injector) context.getApplicationContext()).injectObjects(this);

        setDialogLayoutResource(R.layout.bluetooth_selection_preference);

    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);

        // First, get all required views.
        mProgressBar = (ProgressBar) view.findViewById(R.id
                .bluetooth_selection_preference_search_devices_progressbar);
        mNewDevicesTextView = (TextView) view.findViewById(R.id
                .bluetooth_selection_preference_available_devices_text);
        mPairedDevicesTextView = (TextView) view.findViewById(R.id
                .bluetooth_selection_preference_paired_devices_text);
        mPairedDevicesListView = (ListView) view.findViewById(R.id
                .bluetooth_selection_preference_paired_devices_list);
        mNewDevicesListView = (ListView) view.findViewById(R.id
                .bluetooth_selection_preference_available_devices_list);

        // Initialize the array adapter for both list views
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(getContext(),
                R.layout.bluetooth_selection_preference_device_name);
        mPairedDevicesAdapter = new ArrayAdapter<String>(getContext(),
                R.layout.bluetooth_selection_preference_device_name);


        mNewBluetoothDevices = new ArrayList<>();
        mPairedDevices = new ArrayList<>();

        // Set the adapter for both list views
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);

        // Initialize the toolbar and the menu entry.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.bluetooth_selection_preference_toolbar);
        toolbar.inflateMenu(R.menu.menu_select_bluetooth_preference);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_search_bluetooth_devices:
                        startBluetoothDiscovery();
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });


        updatePairedDevicesList();

        mNewDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothHandler.pairDevice(mNewBluetoothDevices.get(position), new BluetoothHandler.BluetoothDevicePairingCallback() {
                    @Override
                    public void onPairingStarted() {
                        Toast.makeText(getContext(), "Pairing Started", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPairingError() {
                        Toast.makeText(getContext(), "Pairing Error", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

    private void startBluetoothDiscovery() {
        mBluetoothHandler.startBluetoothDeviceDiscovery(new BluetoothHandler
                .BluetoothDeviceDiscoveryCallback() {
            @Override
            public void onActionDeviceDiscoveryStarted() {
                mProgressBar.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Discovery Started!", Toast
                        .LENGTH_LONG).show();
            }

            @Override
            public void onActionDeviceDiscoveryFinished() {
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getContext(), "Discovery Finished!", Toast
                        .LENGTH_LONG).show();
            }

            @Override
            public void onActionDeviceDiscovered(BluetoothDevice device) {
                // if the discovered device is not already part of the list, then
                // add it to the list and add an entry to the array adapter.
                if (!mNewBluetoothDevices.contains(device)) {
                    mNewBluetoothDevices.add(device);
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device
                            .getAddress());
                    Toast.makeText(getContext(), "Device Found!", Toast
                            .LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Updates the list of already paired devices.
     */
    private void updatePairedDevicesList() {
        // Get the set of paired devices.
        Set<BluetoothDevice> pairedDevices = mBluetoothHandler.getPairedBluetoothDevices();

        // For each device, add an entry to the list view.
        for (BluetoothDevice device : pairedDevices) {
            if (!mPairedDevices.contains(device)) {
                mPairedDevices.add(device);
                mPairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        // Make the paired devices textview visible if there are paired devices
        if (!pairedDevices.isEmpty()) {
            mPairedDevicesTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Remove default preference title.
        builder.setTitle(null);
    }

}
