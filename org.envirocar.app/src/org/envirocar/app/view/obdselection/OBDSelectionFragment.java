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
package org.envirocar.app.view.obdselection;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.core.events.bluetooth.BluetoothPairingChangedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;

import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class OBDSelectionFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(OBDSelectionFragment.class);

    /**
     *
     */
    public interface ShowSnackbarListener {
        void showSnackbar(String text);
    }

    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @InjectView(R.id.activity_obd_selection_layout_content)
    protected View mContentView;
    @InjectView(R.id.activity_obd_selection_layout_paired_devices_text)
    protected TextView mPairedDevicesTextView;
    @InjectView(R.id.activity_obd_selection_layout_paired_devices_list)
    protected ListView mPairedDevicesListView;
    @InjectView(R.id.activity_obd_selection_layout_available_devices_list)
    protected ListView mNewDevicesListView;
    @InjectView(R.id.activity_obd_selection_layout_search_devices_progressbar)
    protected ProgressBar mProgressBar;

    @InjectView(R.id.activity_obd_selection_layout_available_devices_info)
    protected TextView mNewDevicesInfoTextView;

    // ArrayAdapter for the two different list views.
    private OBDDeviceListAdapter mNewDevicesArrayAdapter;
    private OBDDeviceListAdapter mPairedDevicesAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        // infalte the content view of this activity.
        View contentView = inflater.inflate(R.layout.activity_obd_selection_fragment,
                container, false);

        // Inject all annotated views.
        ButterKnife.inject(this, contentView);

        // Setup the listviews, its adapters, and its onClick listener.
        setupListViews();

        // Setup the paired devices.
        updatePairedDevicesList();

        // Start the discovery of bluetooth devices.
        updateContentView();

        //        // TODO: very ugly... Instead a dynamic LinearLayout should be used.
        //        setDynamicListHeight(mNewDevicesListView);
        //        setDynamicListHeight(mPairedDevicesListView);

        return contentView;
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
            mNewDevicesInfoTextView.setText(R.string.obd_selection_bluetooth_disabled);
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
     * Initiates the discovery of other Bluetooth devices.
     */
    private void startBluetoothDiscovery() {
        // If bluetooth is not enabled, skip the discovery and show a toast.
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            LOGGER.debug("startBluetoothDiscovery(): Bluetooth is disabled!");
            showSnackbar(getString(R.string.obd_selection_bluetooth_disabled_snackbar));
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
                                showSnackbar(getString(R.string.obd_selection_discovery_started));
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
                                    String string = getString(R.string
                                            .bluetooth_pairing_preference_info_devices_found);
                                    mNewDevicesInfoTextView.setText(String.format(string,
                                            Integer.toString(mNewDevicesArrayAdapter.getCount())));
                                }

                                showSnackbar("Discovery Finished!");
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOGGER.error("Error while discovering bluetooth devices", e);
                            }

                            @Override
                            public void onNext(BluetoothDevice device) {
                                LOGGER.info(String.format(
                                        "Bluetooth device detected: [name=%s, address=%s]",
                                        device.getName(), device.getAddress()));

                                // if the discovered device is not already part of the list, then
                                // add it to the list and add an entry to the array adapter.
                                if (!mPairedDevicesAdapter.contains(device) &&
                                        !mNewDevicesArrayAdapter.contains(device)) {
                                    mNewDevicesArrayAdapter.add(device);
                                }
                            }
                        }
                );
    }

    private void setupListViews() {
        BluetoothDevice selectedBTDevice = mBluetoothHandler.getSelectedBluetoothDevice();

        // Initialize the array adapter for both list views
        mNewDevicesArrayAdapter = new OBDDeviceListAdapter(getActivity(), false);
        mPairedDevicesAdapter = new OBDDeviceListAdapter(getActivity(), true, new
                OBDDeviceListAdapter.OnOBDListActionCallback() {
                    @Override
                    public void onOBDDeviceSelected(BluetoothDevice device) {
                        LOGGER.info(String.format("onOBDDeviceSelected(%s)", device.getName()));

                        // Set the bluetooth device as the selected device in shared preferences.
                        mBluetoothHandler.setSelectedBluetoothDevice(device);

                        // Show a snackbar
                        showSnackbar(String.format(getString(
                                R.string.obd_selection_is_selected_template), device.getName()));
                    }

                    @Override
                    public void onDeleteOBDDevice(BluetoothDevice device) {
                        LOGGER.info(String.format("onDeleteOBDDevice(%s)", device.getName()));
                        showUnpairingDialig(device);
                    }
                }, selectedBTDevice);

        // Set the adapter for both list views
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);


        mNewDevicesListView.setOnItemClickListener((parent, view1, position, id) -> {
            final BluetoothDevice device = mNewDevicesArrayAdapter.getItem(position);

            View contentView = LayoutInflater.from(getActivity()).inflate(R.layout
                    .bluetooth_pairing_preference_device_pairing_dialog, null, false);

            // Set toolbar style
            Toolbar toolbar1 = (Toolbar) contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_toolbar);
            toolbar1.setTitle(R.string.bluetooth_pairing_preference_toolbar_title);
            toolbar1.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
            toolbar1.setTitleTextColor(getActivity().getResources().getColor(R.color
                    .white_cario));

            // Set text view
            TextView textview = (TextView) contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_text);
            textview.setText(String.format(getString(
                    R.string.obd_selection_dialog_pairing_content_template), device.getName()));

            // Create the Dialog
            new AlertDialog.Builder(getActivity())
                    .setView(contentView)
                    .setPositiveButton(R.string.obd_selection_dialog_pairing_title,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // If this button is clicked, pair with the given device
                                    view1.setClickable(false);
                                    pairDevice(device, view1);
                                }
                            })
                    .setNegativeButton(R.string.cancel, null) // Nothing to do on cancel
                    .create()
                    .show();
        });
    }

    private void showUnpairingDialig(BluetoothDevice device) {
        View contentView = LayoutInflater.from(getActivity())
                .inflate(R.layout.bluetooth_pairing_preference_device_pairing_dialog, null, false);

        // Set toolbar style
        Toolbar toolbar1 = (Toolbar) contentView.findViewById(R.id
                .bluetooth_selection_preference_pairing_dialog_toolbar);
        toolbar1.setTitle(R.string.obd_selection_dialog_delete_pairing_title);
        toolbar1.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
        toolbar1.setTitleTextColor(
                getResources().getColor(R.color.white_cario));

        // Set text view
        TextView textview = (TextView) contentView.findViewById(R.id
                .bluetooth_selection_preference_pairing_dialog_text);
        textview.setText(String.format(
                getString(R.string.obd_selection_dialog_delete_pairing_content_template),
                device.getName()));

        // Create the AlertDialog.
        new MaterialDialog.Builder(getActivity())
                .customView(contentView, false)
                .positiveText(R.string.bluetooth_pairing_preference_dialog_remove_pairing)
                .negativeText(R.string.menu_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        LOGGER.debug("OnPositiveButton clicked to remove pairing.");
                        unpairDevice(device);
                    }
                })
                .show();
    }

    private void unpairDevice(BluetoothDevice device) {
        // Try to unpair the device
        mBluetoothHandler.unpairDevice(device,
                new BluetoothHandler.BluetoothDeviceUnpairingCallback() {
                    @Override
                    public void onDeviceUnpaired(BluetoothDevice device) {
                        showSnackbar(String.format(
                                getString(R.string.obd_selection_device_unpaired_template),
                                device.getName() + " (" + device.getAddress() + ")"));
                        mPairedDevicesAdapter.remove(device);
                    }

                    @Override
                    public void onUnpairingError(BluetoothDevice device) {
                        showSnackbar(String.format(
                                getString(R.string.obd_selection_unpairing_error_template),
                                device.getName() + " (" + device.getAddress() + ")"));
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
        mPairedDevicesAdapter.setSelectedBluetoothDevice(mBluetoothHandler
                .getSelectedBluetoothDevice());

        // Make the paired devices textview visible if there are paired devices
        if (!pairedDevices.isEmpty()) {
            mPairedDevicesTextView.setVisibility(View.VISIBLE);
        }
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
                        showSnackbar(getString(R.string.obd_selection_pairing_started));
                        if (text != null) {
                            text.setText(device.getName() + " (Pairing started...)");
                        }
                    }

                    @Override
                    public void onPairingError(BluetoothDevice device) {
                        Toast.makeText(getActivity(), R.string.obd_selection_pairing_error,
                                Toast.LENGTH_LONG).show();
                        if (text != null)
                            text.setText(device.getName());
                    }

                    @Override
                    public void onDevicePaired(BluetoothDevice device) {
                        // Device is paired. Add it to the array adapter for paired devices and
                        // remove it from the adapter for new devices.
                        showSnackbar(String.format(
                                getString(R.string.obd_selection_pairing_success_template),
                                device.getName()));
                        mNewDevicesArrayAdapter.remove(device);
                        mPairedDevicesAdapter.add(device);

                        // Post an event to all registered handlers.
                        mBus.post(new BluetoothPairingChangedEvent(device, true));
                    }
                });
    }

    private void setDynamicListHeight(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }


    /**
     * Shows a snackbar with a given text.
     *
     * @param text the text to show in the snackbar.
     */
    private void showSnackbar(String text) {
        if (getActivity() instanceof OBDSelectionFragment.ShowSnackbarListener)
            ((OBDSelectionFragment.ShowSnackbarListener) getActivity()).showSnackbar(text);
        //        else if(mContentView != null && mContentView.getContext() != null)
        //            Snackbar.make(mContentView, text, Snackbar.LENGTH_LONG).show();
    }

}
