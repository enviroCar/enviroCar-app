package org.envirocar.app.bluetooth.service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

/**
 * @author dewall
 */
public class BluetoothManager {

    private Activity mActivity;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBluetoothIsEnabled;
    private boolean mIsConnected;

    public BluetoothManager(Activity activity){
        this.mActivity = activity;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mBluetoothIsEnabled = mBluetoothAdapter.isEnabled();
        this.mIsConnected = false;
    }
}
