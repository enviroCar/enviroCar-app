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
package org.envirocar.app.views.preferences;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.BaseApplication;
import org.envirocar.app.views.obdselection.OBDSelectionFragment;
import org.envirocar.app.views.preferences.bluetooth.BluetoothDeviceListAdapter;
import org.envirocar.core.events.bluetooth.BluetoothPairingChangedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.logging.Logger;

import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

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
        BaseApplication.get(context).getBaseApplicationComponent().inject(this);

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
        Toolbar toolbar = view.findViewById(R.id.bluetooth_pairing_preference_toolbar);
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


            // Create the Dialog
            new MaterialAlertDialogBuilder(getContext(), R.style.MaterialDialog)
                    .setTitle(R.string.bluetooth_pairing_preference_toolbar_title)
                    .setMessage(String.format(getContext().getString(R.string.obd_selection_dialog_pairing_content_template),device.getName()))
                    .setIcon(R.drawable.ic_bluetooth_white_24dp)
                    .setPositiveButton(R.string.obd_selection_dialog_pairing_title,
                            (dialog, which) -> {
                                // If this button is clicked, pair with the given device
                                view1.setClickable(false);
                                pairDevice(device, view1);
                            })
                    .setNegativeButton(R.string.cancel, null) // Nothing to do on cancel
                    .show();
        });

        // Set an onClickListener for items in the paired devices list.
        mPairedDevicesListView.setOnItemClickListener((parent, view1, position, id) -> {
            final BluetoothDevice device = mPairedDevicesAdapter.getItem(position);

            // Create the AlertDialog.
            new MaterialAlertDialogBuilder(getContext(), R.style.MaterialDialog)
                    .setTitle(R.string.obd_selection_dialog_delete_pairing_title)
                    .setMessage(String.format(getContext().getString(R.string.obd_selection_dialog_delete_pairing_content_template),device.getName()))
                    .setIcon(R.drawable.ic_bluetooth_white_24dp)
                    .setPositiveButton(R.string.bluetooth_pairing_preference_dialog_remove_pairing,
                            (dialog, which) -> {
                                LOGGER.debug("OnPositiveButton clicked to remove pairing.");
                                unpairDevice(device);
                            })
                    .setNegativeButton(R.string.menu_cancel,null) // Nothing to do on cancel
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
        // If bluetooth is not enabled, skip the discovery and show a snackbar.
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            LOGGER.debug("startBluetoothDiscovery(): Bluetooth is disabled!");
            showSnackbar(getContext().getString(R.string.obd_selection_bluetooth_disabled_snackbar));
            return;
        }

        // Before starting a fresh discovery of bluetooth devices, clear
        // the current adapter.
        mNewDevicesArrayAdapter.clear();

//        Subscription sub = mBluetoothHandler.startBluetoothDeviceDiscoveryObservable(true)
        Disposable sub = mBluetoothHandler.startBluetoothDiscoveryOnlyUnpaired()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<BluetoothDevice>() {
                    @Override
                    public void onStart() {
                        LOGGER.info(getContext().getString(R.string.obd_selection_discovery_started));

                        // Show the progressbar
                        mProgressBar.setVisibility(View.VISIBLE);

                        // Set info view to "searching...".
                        mNewDevicesInfoTextView.setText(R.string
                                .bluetooth_pairing_preference_info_searching_devices);

                        showSnackbar(getContext().getString(R.string.obd_selection_discovery_started));
                    }

                    @Override
                    public void onComplete() {
                        LOGGER.info(getContext().getString(R.string.obd_selection_discovery_finished));

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

                        showSnackbar(getContext().getString(R.string.obd_selection_discovery_finished));
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
                        }
                    }
                });
    }


    /**
     * Initiates the pairing process to a given device.
     *
     * @param device the device to pair to.
     * @param view   the view of the listview entry.
     */

    private void pairDevice(BluetoothDevice device, final View view) {
        final TextView text = view.findViewById(R.id
                .bluetooth_selection_preference_device_list_entry_text);

        mBluetoothHandler.pairDevice(device,
                new BluetoothHandler.BluetoothDevicePairingCallback() {

                    @Override
                    public void onPairingStarted(BluetoothDevice device) {
                        showSnackbar(getContext().getString(R.string.obd_selection_pairing_started));
                        if (text != null) {
                            text.setText(device.getName() + " (Pairing started...)");
                        }
                    }

                    @Override
                    public void onPairingError(BluetoothDevice device) {
                        showSnackbar(getContext().getString(R.string.obd_selection_pairing_error));
                        if (text != null)
                            text.setText(device.getName());
                    }

                    @Override
                    public void onDevicePaired(BluetoothDevice device) {
                        // Device is paired. Add it to the array adapter for paired devices and
                        // remove it from the adapter for new devices.
                        showSnackbar(getContext().getString(R.string.obd_selection_paired_successfully));
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

    /**
     * Shows a snackbar with a given text.
     *
     * @param text the text to show in the snackbar.
     */
    private void showSnackbar(String text) {
        if (this instanceof OBDSelectionFragment.ShowSnackbarListener)
            ((OBDSelectionFragment.ShowSnackbarListener) this).showSnackbar(text);
    }

}
