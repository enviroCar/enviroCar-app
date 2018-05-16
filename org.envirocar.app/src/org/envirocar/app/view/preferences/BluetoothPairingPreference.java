/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.preferences;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.view.preferences.bluetooth.BluetoothDeviceListAdapter;
import org.envirocar.core.events.bluetooth.BluetoothPairingChangedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;

import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class BluetoothPairingPreference extends DialogPreference {
    private static final Logger LOGGER = Logger.getLogger(BluetoothPairingPreference.class);

    // Views for the already paired devices.
    @BindView(R.id.bluetooth_pairing_preference_paired_devices_text)
    public TextView mPairedDevicesTextView;
    @BindView(R.id.bluetooth_pairing_preference_paired_devices_list)
    public ListView mPairedDevicesListView;

    // Views for the newly discovered devices.
    @BindView(R.id.bluetooth_pairing_preference_available_devices_text)
    public TextView mNewDevicesTextView;
    @BindView(R.id.bluetooth_pairing_preference_available_devices_list)
    public ListView mNewDevicesListView;

    // No device found.
    @BindView(R.id.bluetooth_pairing_preference_available_devices_info)
    public TextView mNewDevicesInfoTextView;
    @BindView(R.id.bluetooth_pairing_preference_search_devices_progressbar)
    public ProgressBar mProgressBar;

    // Injected variables.
    @Inject
    protected Bus mBus;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    // Main parent view for the content.
    @BindView(R.id.bluetooth_pairing_preference_content)
    protected LinearLayout mContentView;

    // ArrayAdapter for the two different list views.
    private BluetoothDeviceListAdapter mNewDevicesArrayAdapter;
    private BluetoothDeviceListAdapter mPairedDevicesAdapter;

    /**
     * Constructor.
     *
     * @param context the Context of the current scope.
     * @param attrs   the attribute set.
     */
    public BluetoothPairingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Inject fields.
        ((Injector) context.getApplicationContext()).injectObjects(this);

        // Set the layout of the dialog to show.
        setDialogLayoutResource(R.layout.bluetooth_pairing_preference);
    }

    /**
     * Binds views in the content View of the dialog to data.
     *
     * @param view the content view of the dialog.
     */
    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);

        // Inject all views.
        ButterKnife.bind(this, view);

        // Initialize the array adapter for both list views
        mNewDevicesArrayAdapter = new BluetoothDeviceListAdapter(getContext(),
                R.layout.bluetooth_pairing_preference_device_name, false);
        mPairedDevicesAdapter = new BluetoothDeviceListAdapter(getContext(),
                R.layout.bluetooth_pairing_preference_device_name, true);

        // Set the adapter for both list views
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);

        // Initialize the toolbar and the menu entry.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.bluetooth_pairing_preference_toolbar);
        toolbar.setTitle(R.string.bluetooth_pairing_preference_toolbar_title);
        toolbar.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
        toolbar.inflateMenu(R.menu.menu_select_bluetooth_preference);
        toolbar.setTitleTextColor(getContext().getResources().getColor(R.color
                .white_cario));
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_action_search_bluetooth_devices:
                    startBluetoothDiscovery();
                    return true;
                default:
                    break;
            }
            return false;
        });

        // Updates the list of already paired devices.
        updatePairedDevicesList();

        mNewDevicesListView.setOnItemClickListener((parent, view1, position, id) -> {
            final BluetoothDevice device = mNewDevicesArrayAdapter.getItem(position);

            View contentView = LayoutInflater.from(getContext()).inflate(R.layout
                    .bluetooth_pairing_preference_device_pairing_dialog, null, false);

            // Set toolbar style
            Toolbar toolbar1 = (Toolbar) contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_toolbar);
            toolbar1.setTitle(R.string.bluetooth_pairing_preference_toolbar_title);
            toolbar1.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
            toolbar1.setTitleTextColor(getContext().getResources().getColor(R.color
                    .white_cario));

            // Set text view
            TextView textview = (TextView) contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_text);
            textview.setText(String.format("Do you want to pair with %s?", device.getName()));

            // Create the Dialog
            new AlertDialog.Builder(getContext())
                    .setView(contentView)
                    .setPositiveButton("Pair Device", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // If this button is clicked, pair with the given device
                            view1.setClickable(false);
                            pairDevice(device, view1);
                        }
                    })
                    .setNegativeButton("Cancel", null) // Nothing to do on cancel
                    .create()
                    .show();
        });

        // Set an onClickListener for items in the paired devices list.
        mPairedDevicesListView.setOnItemClickListener((parent, view1, position, id) -> {
            final BluetoothDevice device = mPairedDevicesAdapter.getItem(position);

            View contentView = LayoutInflater.from(getContext()).inflate(R.layout
                    .bluetooth_pairing_preference_device_pairing_dialog, null, false);

            // Set toolbar style
            Toolbar toolbar1 = (Toolbar) contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_toolbar);
            toolbar1.setTitle("Bluetooth Device");
            toolbar1.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
            toolbar1.setTitleTextColor(getContext().getResources().getColor(R.color
                    .white_cario));

            // Set text view
            TextView textview = (TextView) contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_text);
            textview.setText(String.format("Do you want to remove the pairing with %s?", device
                    .getName()));

            // Create the AlertDialog.
            new AlertDialog.Builder(getContext())
                    .setView(contentView)
                    .setPositiveButton(R.string.bluetooth_pairing_preference_dialog_remove_pairing,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    LOGGER.debug("OnPositiveButton clicked for remove pairing.");
                                    unpairDevice(device);
                                }
                            })
                    .setNegativeButton(R.string.menu_cancel, null) // Nothing to do on
                            // cancel.
                    .create()
                    .show();
        });

        // Register this object on the event bus
        mBus.register(this);

        // Start the discovery of bluetooth devices.
        updateContentView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Stop the discovery if active.
        mBluetoothHandler.stopBluetoothDeviceDiscovery();

        // Unregister this instance from the eventBus
        mBus.unregister(this);

        super.onDismiss(dialog);
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.debug("onBluetoothStateChangedEvent(): " + event.toString());
        updateContentView();
    }

    /**
     * Updates the content view.
     */
    private void updateContentView() {
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            // Bluetooth is not enabled. Disable the content view and update the info text view.
            mContentView.setVisibility(View.GONE);
            mNewDevicesArrayAdapter.clear();
            mPairedDevicesAdapter.clear();
            mNewDevicesInfoTextView.setText("Bluetooth is disabled.");
        } else {
            // Bluetooth is enabled. Show the content view, update the list, and start the
            // discovery of Bluetooth devices.
            mNewDevicesArrayAdapter.clear();
            mPairedDevicesAdapter.clear();
            mContentView.setVisibility(View.VISIBLE);
            updatePairedDevicesList();
            startBluetoothDiscovery();
        }
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

    /**
     * Initiates the discovery of other Bluetooth devices.
     */
    private void startBluetoothDiscovery() {
        // If bluetooth is not enabled, skip the discovery and show a toast.
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            LOGGER.debug("startBluetoothDiscovery(): Bluetooth is disabled!");
            Toast.makeText(getContext(), "Bluetooth is disabled. Please enable Bluetooth before " +
                    "discovering for other devices.", Toast.LENGTH_LONG).show();
            return;
        }

        // Before starting a fresh discovery of bluetooth devices, clear
        // the current adapter.
        mNewDevicesArrayAdapter.clear();

//        Subscription sub = mBluetoothHandler.startBluetoothDeviceDiscoveryObservable(true)
        Subscription sub = mBluetoothHandler.startBluetoothDiscoveryOnlyUnpaired()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Subscriber<BluetoothDevice>() {
                            @Override
                            public void onStart() {
                                LOGGER.info("Blutooth discovery started.");

                                // Show the progressbar
                                mProgressBar.setVisibility(View.VISIBLE);

                                // Set info view to "searching...".
                                mNewDevicesInfoTextView.setText(R.string
                                        .bluetooth_pairing_preference_info_searching_devices);

                                Toast.makeText(getContext(), "Discovery Started!", Toast
                                        .LENGTH_LONG).show();
                            }

                            @Override
                            public void onCompleted() {
                                LOGGER.info("Bluetooth discovery finished.");

                                // Dismiss the progressbar.
                                mProgressBar.setVisibility(View.GONE);

                                // If no devices found, set the corresponding textview to visibile.
                                if (mNewDevicesArrayAdapter.isEmpty()) {
                                    mNewDevicesInfoTextView.setText(R.string
                                            .select_bluetooth_preference_info_no_device_found);
                                } else if (mNewDevicesArrayAdapter.getCount() == 1) {
                                    mNewDevicesInfoTextView.setText(R.string
                                            .bluetooth_pairing_preference_info_device_found);
                                } else {
                                    String string = getContext().getString(R.string
                                            .bluetooth_pairing_preference_info_devices_found);
                                    mNewDevicesInfoTextView.setText(String.format(string, "" +
                                            mNewDevicesArrayAdapter.getCount()));
                                }

                                Toast.makeText(getContext(), "Discovery Finished!", Toast
                                        .LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOGGER.error("Error while discovering bluetooth devices", e);
                            }

                            @Override
                            public void onNext(BluetoothDevice device) {
                                LOGGER.info(String.format("Bluetooth device detected: [name=%s, address=%s]",
                                        device.getName(), device.getAddress()));

                                // if the discovered device is not already part of the list, then
                                // add it to the list and add an entry to the array adapter.
                                if (!mPairedDevicesAdapter.contains(device) &&
                                        !mNewDevicesArrayAdapter.contains(device)) {
                                    mNewDevicesArrayAdapter.add(device);
//                }
                                }
                            }
                        }
                );
    }


    /**
     * Initiates the pairing process to a given device.
     *
     * @param device the device to pair to.
     * @param view   the view of the listview entry.
     */

    private void pairDevice(BluetoothDevice device, final View view) {
        final TextView text = (TextView) view.findViewById(R.id
                .bluetooth_selection_preference_device_list_entry_text);

        mBluetoothHandler.pairDevice(device,
                new BluetoothHandler.BluetoothDevicePairingCallback() {

                    @Override
                    public void onPairingStarted(BluetoothDevice device) {
                        Toast.makeText(getContext(), "Pairing Started",
                                Toast.LENGTH_LONG).show();
                        if (text != null) {
                            text.setText(device.getName() + " (Pairing started...)");
                        }
                    }

                    @Override
                    public void onPairingError(BluetoothDevice device) {
                        Toast.makeText(getContext(), "Pairing Error",
                                Toast.LENGTH_LONG).show();
                        if (text != null)
                            text.setText(device.getName());
                    }

                    @Override
                    public void onDevicePaired(BluetoothDevice device) {
                        // Device is paired. Add it to the array adapter for paired devices and
                        // remove it from the adapter for new devices.
                        Toast.makeText(getContext(), "Paired", Toast.LENGTH_LONG).show();
                        mNewDevicesArrayAdapter.remove(device);
                        mPairedDevicesAdapter.add(device);

                        // Post an event to all registered handlers.
                        mBus.post(new BluetoothPairingChangedEvent(device, true));
                    }
                });
    }

    /**
     * @param device
     */
    private void unpairDevice(BluetoothDevice device) {
        LOGGER.debug("unpairDevice(): remove the pairing for device " + device.getName());

        // Call the unpairing procedure at the bluetoothhandler with a respective callback.
        mBluetoothHandler.unpairDevice(device, new BluetoothHandler
                .BluetoothDeviceUnpairingCallback() {
            @Override
            public void onDeviceUnpaired(BluetoothDevice device) {
                LOGGER.debug(String.format("unpairDevice(): %s successfully unpaired", device
                        .getName()));

                // Remove the unpaired device if it is contained in the adapter.
                if (mPairedDevicesAdapter.contains(device)) {
                    mPairedDevicesAdapter.remove(device);
                }

                // Posts an event to all registered handlers.
                mBus.post(new BluetoothPairingChangedEvent(device, false));
            }

            @Override
            public void onUnpairingError(BluetoothDevice device) {
                LOGGER.debug(String.format("unpairDevice(): error while unpairing device %s",
                        device.getName()));
            }
        });
    }



    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Remove default preference title.
        builder.setTitle(null);
    }

}
