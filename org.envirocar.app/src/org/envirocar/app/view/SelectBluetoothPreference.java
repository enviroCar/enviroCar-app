package org.envirocar.app.view;

import android.app.Activity;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;

import org.envirocar.app.Injector;
import org.envirocar.app.R;
import org.envirocar.app.bluetooth.service.BluetoothHandler;
import org.envirocar.app.view.bluetooth.BluetoothDeviceDialogFragment;
import org.envirocar.app.view.bluetooth.BluetoothDeviceListAdapter;

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
    private BluetoothDeviceListAdapter mNewDevicesArrayAdapter;
    private BluetoothDeviceListAdapter mPairedDevicesAdapter;

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

    /**
     * Binds views in the content View of the dialog to data.
     *
     * @param view the content view of the dialog.
     */
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
        mNewDevicesTextView = (TextView) view.findViewById(R.id
                .bluetooth_selection_preference_available_devices_text);
        mNewDevicesListView = (ListView) view.findViewById(R.id
                .bluetooth_selection_preference_available_devices_list);

        // Initialize the array adapter for both list views
        mNewDevicesArrayAdapter = new BluetoothDeviceListAdapter(getContext(),
                R.layout.bluetooth_selection_preference_device_name, false,
                new BluetoothDeviceListAdapter.BluetoothDeviceListButtonListener() {
            @Override
            public void onButtonClicked(BluetoothDevice device) {

            }
        });
        mPairedDevicesAdapter = new BluetoothDeviceListAdapter(getContext(),
                R.layout.bluetooth_selection_preference_device_name, true,
                new BluetoothDeviceListAdapter.BluetoothDeviceListButtonListener() {
            @Override
            public void onButtonClicked(BluetoothDevice device) {

            }
        });

        // Set the adapter for both list views
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);

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

        // Updates the list of already paired devices.
        updatePairedDevicesList();

        mNewDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothHandler.pairDevice(mNewDevicesArrayAdapter.getItem(position),
                        new BluetoothHandler.BluetoothDevicePairingCallback() {

                            @Override
                            public void onPairingStarted(BluetoothDevice device) {
                                Toast.makeText(getContext(), "Pairing Started",
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onPairingError(BluetoothDevice device) {
                                Toast.makeText(getContext(), "Pairing Error",
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onDevicePaired(BluetoothDevice device) {
                                Toast.makeText(getContext(), "Paired", Toast.LENGTH_LONG).show();
                                mNewDevicesArrayAdapter.remove(device);
                                mPairedDevicesAdapter.add(device);
                            }
                        });
            }
        });

        mPairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDeviceDialogFragment.newInstance(
                        mPairedDevicesAdapter.getItem(position))
                        .show(((Activity) getContext()).getFragmentManager(), "wurst");
            }
        });
    }

    /**
     * Initiates the discovery of other Bluetooth devices.
     */
    private void startBluetoothDiscovery() {
        // Before starting a fresh discovery of bluetooth devices, clear
        // the current adapter.
        mNewDevicesArrayAdapter.clear();

        // Start the discovery.
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
                mProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Discovery Finished!", Toast
                        .LENGTH_LONG).show();
            }

            @Override
            public void onActionDeviceDiscovered(BluetoothDevice device) {
                // if the discovered device is not already part of the list, then
                // add it to the list and add an entry to the array adapter.
                if (!mPairedDevicesAdapter.contains(device)) {
                    mNewDevicesArrayAdapter.add(device);
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
        mPairedDevicesAdapter.addAll(pairedDevices);

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
