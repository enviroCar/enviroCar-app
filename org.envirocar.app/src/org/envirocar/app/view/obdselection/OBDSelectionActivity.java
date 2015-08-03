package org.envirocar.app.view.obdselection;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.injection.BaseInjectorActivity;

import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @dewall
 */
public class OBDSelectionActivity extends BaseInjectorActivity {

    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @InjectView(R.id.activity_obd_selection_layout_toolbar)
    protected Toolbar mToolbar;

    @InjectView(R.id.activity_obd_selection_layout_paired_devices_text)
    public TextView mPairedDevicesTextView;

    @InjectView(R.id.activity_obd_selection_layout_paired_devices_list)
    protected ListView mPairedDevicesListView;
    @InjectView(R.id.activity_obd_selection_layout_available_devices_list)
    protected ListView mNewDevicesListView;


    // ArrayAdapter for the two different list views.
    private OBDDeviceListAdapter mNewDevicesArrayAdapter;
    private OBDDeviceListAdapter mPairedDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of this activity.
        setContentView(R.layout.activity_obd_selection_layout);

        // Inject all annotated views.
        ButterKnife.inject(this);

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        BluetoothDevice selectedBTDevice = mBluetoothHandler.getSelectedBluetoothDevice();

        // Initialize the array adapter for both list views
        mNewDevicesArrayAdapter = new OBDDeviceListAdapter(this, false);
        mPairedDevicesAdapter = new OBDDeviceListAdapter(this, true, new
                OBDDeviceListAdapter.OnOBDListActionCallback() {
                    @Override
                    public void onOBDDeviceSelected(BluetoothDevice device) {
                        Snackbar.make(mToolbar, "Device selected " + device.getName(), Snackbar
                                .LENGTH_LONG).show();
                    }

                    @Override
                    public void onDeleteOBDDevice(BluetoothDevice device) {
                        Snackbar.make(mToolbar, String.format("%s has been deleted.", device
                                .getName()), Snackbar.LENGTH_LONG).show();
                    }
                }, selectedBTDevice);

        // Set the adapter for both list views
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        mPairedDevicesListView.setAdapter(mPairedDevicesAdapter);

        updatePairedDevicesList();


        mPairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mPairedDevicesAdapter.getItem(position);

                new MaterialDialog.Builder(OBDSelectionActivity.this)
                        .title(device.getName())
                        .items(R.array.car_list_option_items)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int
                                    i, CharSequence charSequence) {
                                switch (i) {
                                    case 0:

                                        break;
                                    case 1:

                                        break;
                                }

                            }
                        })
                        .show();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // click on the home button in the toolbar.
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *
     * @param device
     */
    public void setSelectedBluetoothAdapter(BluetoothDevice device){

    }

    private void showSnackbar(String text) {
        Snackbar.make(mToolbar, text, Snackbar.LENGTH_LONG).show();
    }
}
