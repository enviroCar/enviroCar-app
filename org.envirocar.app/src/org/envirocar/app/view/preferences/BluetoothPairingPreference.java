package org.envirocar.app.view.preferences;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.Injector;
import org.envirocar.app.R;
import org.envirocar.app.bluetooth.event.BluetoothPairingChangedEvent;
import org.envirocar.app.bluetooth.event.BluetoothStateChangedEvent;
import org.envirocar.app.bluetooth.service.BluetoothHandler;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.view.preferences.bluetooth.BluetoothDeviceListAdapter;

import java.util.Set;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class BluetoothPairingPreference extends DialogPreference {
    private static final Logger LOGGER = Logger.getLogger(BluetoothPairingPreference.class);

    // Injected variables.
    @Inject
    protected Bus mBus;

    @Inject
    protected BluetoothHandler mBluetoothHandler;

    // Newly discovered devices
    private BluetoothDeviceListAdapter mNewDevicesArrayAdapter;
    private BluetoothDeviceListAdapter mPairedDevicesAdapter;

    // Main parent view for the content.
    private LinearLayout mContentView;

    // Views for the already paired devices.
    private TextView mPairedDevicesTextView;
    private ListView mPairedDevicesListView;

    // Views for the newly discovered devices.
    private TextView mNewDevicesTextView;
    private ListView mNewDevicesListView;

    // No device found.
    private TextView mNewDevicesInfoTextView;


    private ProgressBar mProgressBar;

    /**
     * Constructor.
     *
     * @param context the Context of the current scope.
     * @param attrs   the attribute set.
     */
    public BluetoothPairingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        ((Injector) context.getApplicationContext()).injectObjects(this);

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

        // First, get all required views.
        mProgressBar = (ProgressBar) view.findViewById(R.id
                .bluetooth_pairing_preference_search_devices_progressbar);
        mContentView = (LinearLayout) view.findViewById(R.id
                .bluetooth_pairing_preference_content);
        mNewDevicesTextView = (TextView) view.findViewById(R.id
                .bluetooth_pairing_preference_available_devices_text);
        mPairedDevicesTextView = (TextView) view.findViewById(R.id
                .bluetooth_pairing_preference_paired_devices_text);
        mPairedDevicesListView = (ListView) view.findViewById(R.id
                .bluetooth_pairing_preference_paired_devices_list);
        mNewDevicesTextView = (TextView) view.findViewById(R.id
                .bluetooth_pairing_preference_available_devices_text);
        mNewDevicesListView = (ListView) view.findViewById(R.id
                .bluetooth_pairing_preference_available_devices_list);
        mNewDevicesInfoTextView = (TextView) view.findViewById(R.id
                .bluetooth_pairing_preference_available_devices_info);

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
        toolbar.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
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
            public void onItemClick(AdapterView<?> parent, final View view,
                                    final int position, long id) {
                final BluetoothDevice device = mNewDevicesArrayAdapter.getItem(position);

                View contentView = LayoutInflater.from(getContext()).inflate(R.layout
                        .bluetooth_pairing_preference_device_pairing_dialog, null, false);

                // Set toolbar style
                Toolbar toolbar = (Toolbar) contentView.findViewById(R.id
                        .bluetooth_selection_preference_pairing_dialog_toolbar);
                toolbar.setTitle(R.string.bluetooth_pairing_preference_toolbar_title);
                toolbar.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
                toolbar.setTitleTextColor(getContext().getResources().getColor(R.color
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
                                view.setClickable(false);
                                pairDevice(device, view);
                            }
                        })
                        .setNegativeButton("Cancel", null) // Nothing to do on cancel
                        .create()
                        .show();
            }
        });

        // Set an onClickListener for items in the paired devices list.
        mPairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mPairedDevicesAdapter.getItem(position);

                View contentView = LayoutInflater.from(getContext()).inflate(R.layout
                        .bluetooth_pairing_preference_device_pairing_dialog, null, false);

                // Set toolbar style
                Toolbar toolbar = (Toolbar) contentView.findViewById(R.id
                        .bluetooth_selection_preference_pairing_dialog_toolbar);
                toolbar.setTitle("Bluetooth Device");
                toolbar.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
                toolbar.setTitleTextColor(getContext().getResources().getColor(R.color
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
            }
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

        // Start the discovery.
        mBluetoothHandler.startBluetoothDeviceDiscovery(new BluetoothHandler
                .BluetoothDeviceDiscoveryCallback() {
            @Override
            public void onActionDeviceDiscoveryStarted() {
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
            public void onActionDeviceDiscoveryFinished() {
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
            public void onActionDeviceDiscovered(final BluetoothDevice device) {
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
