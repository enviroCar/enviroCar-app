package org.envirocar.app.fragments;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.envirocar.app.R;

import java.util.Set;

/**
 * @author dewall
 */
public class BluetoothDeviceListFragment extends DialogFragment {

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // If the discovery found a new device...
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // if the discovered device is already paired, then do nothing because its already
                // in the list.
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
//                    String
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
        }
    };


    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mPairedAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();

        if(bondedDevices.size() > 0){
            for(BluetoothDevice device : bondedDevices){
                mPairedAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = (View) inflater.inflate(R.layout.bluetooth_device_list_fragment, container,
                false);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mBluetoothAdapter != null){
            mBluetoothAdapter.cancelDiscovery();
        }

//        getActivity().getApplicationContext().unregisterReceiver();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


}
