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
package org.envirocar.app.views.obdselection;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.core.events.bluetooth.BluetoothPairingChangedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class OBDSelectionFragment extends BaseInjectorFragment implements EasyPermissions.PermissionCallbacks {
    private static final Logger LOGGER = Logger.getLogger(OBDSelectionFragment.class);

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    /**
     *
     */
    public interface ShowSnackbarListener {
        void showSnackbar(String text);
    }

    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @BindView(R.id.activity_obd_selection_layout_content)
    protected View mContentView;
    @BindView(R.id.activity_obd_selection_layout_paired_devices_text)
    protected TextView mPairedDevicesTextView;
    @BindView(R.id.activity_obd_selection_layout_paired_devices_list)
    protected ListView mPairedDevicesListView;
    @BindView(R.id.activity_obd_selection_layout_available_devices_list)
    protected ListView mNewDevicesListView;
    @BindView(R.id.activity_obd_selection_layout_search_devices_progressbar)
    protected ProgressBar mProgressBar;
    @BindView(R.id.activity_obd_selection_layout_rescan_bluetooth)
    protected ImageView mRescanImageView;

    @BindView(R.id.activity_obd_selection_layout_paired_devices_info)
    protected TextView mPairedDevicesInfoTextView;
    @BindView(R.id.activity_obd_selection_layout_available_devices_info)
    protected TextView mNewDevicesInfoTextView;

    // ArrayAdapter for the two different list views.
    private OBDDeviceListAdapter mNewDevicesArrayAdapter;
    private OBDDeviceListAdapter mPairedDevicesAdapter;

    private Disposable mBTDiscoverySubscription;

    private boolean isResumed = false;
    public boolean pairingIsRunning = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // infalte the content view of this activity.
        View contentView = inflater.inflate(R.layout.activity_obd_selection_fragment,
                container, false);

        // Inject all annotated views.
        ButterKnife.bind(this, contentView);

        // Setup the listviews, its adapters, and its onClick listener.
        setupListViews();

        // Check the GPS and Location permissions
        // before Starting the discovery of bluetooth devices.
        updateContentView();

        //        // TODO: very ugly... Instead a dynamic LinearLayout should be used.
        //        setDynamicListHeight(mNewDevicesListView);
        //        setDynamicListHeight(mPairedDevicesListView);

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndRequestPermissions();
    }

    @Override
    public void onDestroy() {
        if (mBTDiscoverySubscription != null && !mBTDiscoverySubscription.isDisposed()) {
            mBTDiscoverySubscription.dispose();
        }

        super.onDestroy();
    }

    @Subscribe
    public void onBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        getActivity().getWindow().getDecorView().post(() -> {
            LOGGER.debug("onBluetoothStateChangedEvent(): " + event.toString());
            updateContentView();
        });
    }

    @OnClick(R.id.activity_obd_selection_layout_rescan_bluetooth)
    protected void rediscover() {
        mBluetoothHandler.stopBluetoothDeviceDiscovery();
        checkAndRequestPermissions();
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
        } else {
            // Bluetooth is enabled. Show the content view, update the list, and start the
            // discovery of Bluetooth devices.
            mNewDevicesArrayAdapter.clear();
            mPairedDevicesAdapter.clear();
            mContentView.setVisibility(View.VISIBLE);
            updatePairedDevicesList();
        }
    }

    private final int BLUETOOTH_PERMISSIONS = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    public void checkAndRequestPermissions() {
        String[] perms;
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            perms = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        }
        else{
            perms = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        if (EasyPermissions.hasPermissions(getContext(), perms)){
            // if all permissions are granted, start bluetooth discovery.
            startBluetoothDiscovery();
        }
        else{
            // Dialog requesting the user for location permission.
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, BLUETOOTH_PERMISSIONS, perms)
                            .setRationale(R.string.location_permission_to_discover_newdevices)
                            .setPositiveButtonText(R.string.grant_permissions)
                            .setNegativeButtonText(R.string.cancel)
                            .setTheme(R.style.MaterialDialog)
                            .build());
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull @NotNull List<String> perms) {
        // if location permissions are granted, start Bluetooth discovery.
        if (requestCode == BLUETOOTH_PERMISSIONS) {
            startBluetoothDiscovery();
            showSnackbar(getString(R.string.location_permission_granted));
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull @NotNull List<String> perms) {
        // if permissions are not granted, show toast.
        if (requestCode == BLUETOOTH_PERMISSIONS) {
            showSnackbar(getString(R.string.location_permission_denied));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;
    }

    /**
     * Initiates the discovery of other Bluetooth devices.
     */
    private void startBluetoothDiscovery(){
        // If bluetooth is not enabled, skip the discovery and show a toast.
        if (!mBluetoothHandler.isBluetoothEnabled()) {
            LOGGER.debug("startBluetoothDiscovery(): Bluetooth is disabled!");
            showSnackbar(getString(R.string.obd_selection_bluetooth_disabled_snackbar));
            return;
        }

        // Before starting a fresh discovery of bluetooth devices, clear
        // the current adapter.
        mNewDevicesArrayAdapter.clear();

        mBTDiscoverySubscription = mBluetoothHandler.startBluetoothDiscoveryOnlyUnpaired()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnDispose(() -> {
                    LOGGER.info("Canceling bluetooth device discovery");
                    mBluetoothHandler.stopBluetoothDeviceDiscovery();
                })
                .subscribeWith(new DisposableObserver<BluetoothDevice>() {

                    @Override
                    public void onStart() {
                        LOGGER.info("Blutooth discovery started.");

                        // Show the progressbar and remove rescan option
                        mProgressBar.setVisibility(View.VISIBLE);
                        mRescanImageView.setVisibility(View.GONE);

                        showSnackbar(getString(R.string.obd_selection_discovery_started));

                        // Set timer for 15sec
                        new CountDownTimer(15000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                            }
                            @Override
                            public void onFinish() {
                                // If discovering of a device takes more than 15 sec ,then show user discovery is finished
                                // if the needed device is not found , rediscover button can be used.
                                if (mBluetoothHandler.isDiscovering()) {
                                    mBluetoothHandler.stopBluetoothDeviceDiscovery();
                                }
                            }
                        }.start();
                    }

                    @Override
                    public void onComplete() {
                        LOGGER.info("Bluetooth discovery finished.");

                        mProgressBar.setVisibility(View.GONE);
                        mRescanImageView.setVisibility(View.VISIBLE);
                        showSnackbar(getString(R.string.obd_selection_discovery_finished));
                        if(mNewDevicesArrayAdapter.isEmpty()){
                            mNewDevicesInfoTextView.setVisibility(View.VISIBLE);
                        }

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
                            mNewDevicesInfoTextView.setVisibility(View.GONE);
                            mNewDevicesArrayAdapter.add(device);
                        }
                    }
                });
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
                        showUnpairingDialog(device);
                    }
                }, selectedBTDevice);

        // Set the adapter for both list views
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);


        mNewDevicesListView.setOnItemClickListener((parent, view1, position, id) -> {
            final BluetoothDevice device = mNewDevicesArrayAdapter.getItem(position);

            // Create the Dialog
            new MaterialAlertDialogBuilder(getActivity(), R.style.MaterialDialog)
                    .setTitle(R.string.bluetooth_pairing_preference_toolbar_title)
                    .setMessage(String.format(getString(
                            R.string.obd_selection_dialog_pairing_content_template), device.getName()))
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
    }

    private void showUnpairingDialog(BluetoothDevice device) {
        // Create the AlertDialog.
        new MaterialAlertDialogBuilder(getActivity(), R.style.MaterialDialog)
                .setTitle(R.string.obd_selection_dialog_delete_pairing_title)
                .setMessage(String.format(getString(R.string.obd_selection_dialog_delete_pairing_content_template),device.getName()))
                .setIcon(R.drawable.ic_bluetooth_white_24dp)
                .setPositiveButton(R.string.bluetooth_pairing_preference_dialog_remove_pairing,
                        (dialog, which) -> {
                            LOGGER.debug("OnPositiveButton clicked to remove pairing.");
                            unpairDevice(device);
                        })
                .setNegativeButton(R.string.menu_cancel,null) // Nothing to do on cancel
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
                        if (mPairedDevicesAdapter.getCount() == 0 ){
                            mPairedDevicesInfoTextView.setVisibility(View.VISIBLE);
                            //mPairedDevicesTextView.setVisibility(View.GONE);
                        }
                        updatePairedDevicesList();
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

        mPairedDevicesAdapter.clear();
        // For each device, add an entry to the list view.
        mPairedDevicesAdapter.addAll(pairedDevices);
        mPairedDevicesAdapter.setSelectedBluetoothDevice(mBluetoothHandler
                .getSelectedBluetoothDevice());

        // Make the paired devices textview visible if there are paired devices
        if (!pairedDevices.isEmpty()) {
            mPairedDevicesInfoTextView.setVisibility(View.GONE);
            //mPairedDevicesTextView.setVisibility(View.VISIBLE);
        }
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
                        pairingIsRunning = true;
                        showSnackbar(getString(R.string.obd_selection_pairing_started));
                        if (text != null) {
                            text.setText(device.getName() + " (Pairing started...)");
                        }
                    }

                    @Override
                    public void onPairingError(BluetoothDevice device) {
                        pairingIsRunning = false;
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    R.string.obd_selection_pairing_error,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (text != null)
                            text.setText(device.getName());
                    }

                    @Override
                    public void onDevicePaired(BluetoothDevice device) {
                        pairingIsRunning = false;
                        // Device is paired. Add it to the array adapter for paired devices and
                        // remove it from the adapter for new devices.
                        showSnackbar(String.format(
                                getString(R.string.obd_selection_pairing_success_template),
                                device.getName()));
                        // TODO Issue: Unstable bluetooth connect workflow #844
                        //  --> under the in the issue explained circumstances the getString()-methode
                        //  fails at this point because the fragment has no context

                        mNewDevicesArrayAdapter.remove(device);
                        mPairedDevicesAdapter.add(device);

                        mPairedDevicesInfoTextView.setVisibility(View.GONE);
                        if (mNewDevicesArrayAdapter.isEmpty()) {
                            mNewDevicesInfoTextView.setVisibility(View.VISIBLE);
                        }

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
        if (getActivity() instanceof ShowSnackbarListener)
            ((ShowSnackbarListener) getActivity()).showSnackbar(text);
        //        else if(mContentView != null && mContentView.getContext() != null)
        //            Snackbar.make(mContentView, text, Snackbar.LENGTH_LONG).show();
    }

}
